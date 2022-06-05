package com.linkedin.metadata.boot;

import com.datastax.oss.driver.api.core.CqlSession;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.identity.CorpUserInfo;
import com.linkedin.metadata.CassandraTestUtils;
import com.linkedin.metadata.boot.steps.IngestDataPlatformInstancesStep;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.RetentionService;
import com.linkedin.metadata.entity.TestEntityRegistry;
import com.linkedin.metadata.entity.cassandra.CassandraAspectDao;
import com.linkedin.metadata.entity.cassandra.CassandraRetentionService;
import com.linkedin.metadata.event.EventProducer;
import com.linkedin.metadata.models.registry.ConfigEntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistryException;
import com.linkedin.metadata.models.registry.MergedEntityRegistry;
import com.linkedin.metadata.snapshot.Snapshot;
import com.linkedin.metadata.utils.PegasusUtils;
import com.linkedin.mxe.SystemMetadata;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.elasticsearch.common.collect.Set;
import org.testcontainers.containers.CassandraContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;


public class CassandraIngestDataPlatformInstancesTest {

  protected static final AuditStamp TEST_AUDIT_STAMP = createTestAuditStamp();
  protected final EntityRegistry _snapshotEntityRegistry;
  protected final EntityRegistry _configEntityRegistry;
  protected final EntityRegistry _testEntityRegistry;
  protected EventProducer _mockProducer;

  private CassandraAspectDao _aspectDao;

  private IngestDataPlatformInstancesStep _step;
  private CassandraContainer _cassandraContainer;
  private EntityService _entityService;
  private RetentionService _retentionService;

  public CassandraIngestDataPlatformInstancesTest() throws EntityRegistryException {
    _snapshotEntityRegistry = new TestEntityRegistry();
    _configEntityRegistry = new ConfigEntityRegistry(Snapshot.class.getClassLoader().getResourceAsStream("entity-registry.yml"));
    _testEntityRegistry = new MergedEntityRegistry(_snapshotEntityRegistry).apply(_configEntityRegistry);
  }

  @BeforeClass
  public void setupContainer() {
    _cassandraContainer = CassandraTestUtils.setupContainer();
  }

  @AfterClass
  public void tearDown() {
    _cassandraContainer.stop();
  }

  @BeforeMethod
  public void setupTest() {
    CassandraTestUtils.purgeData(_cassandraContainer);
    configureComponents();
  }

  private void configureComponents() {
    CqlSession session = CassandraTestUtils.createTestSession(_cassandraContainer);
    _aspectDao = new CassandraAspectDao(session);
    _aspectDao.setConnectionValidated(true);
    _mockProducer = mock(EventProducer.class);
    _entityService = new EntityService(_aspectDao, _mockProducer, _testEntityRegistry);
    _retentionService = new CassandraRetentionService(_entityService, session, 1000);
    _entityService.setRetentionService(_retentionService);
    _step = new IngestDataPlatformInstancesStep(_entityService, _aspectDao);
  }

  @Test
  public void testIt() throws Exception {
    Urn entityUrn1 = Urn.createFromString("urn:li:corpuser:test1");
    Urn entityUrn2 = Urn.createFromString("urn:li:corpuser:test2");
    Urn entityUrn3 = Urn.createFromString("urn:li:corpuser:test3");

    SystemMetadata metadata1 = new SystemMetadata();
    metadata1.setLastObserved(1625792689);
    metadata1.setRunId("run-123");

    String aspectName = PegasusUtils.getAspectNameFromSchema(new CorpUserInfo().schema());

    // Ingest CorpUserInfo Aspect #1
    CorpUserInfo writeAspect1 = createCorpUserInfo("email@test.com");
    _entityService.ingestAspect(entityUrn1, aspectName, writeAspect1, TEST_AUDIT_STAMP, metadata1);

    // Ingest CorpUserInfo Aspect #2
    CorpUserInfo writeAspect2 = createCorpUserInfo("email2@test.com");
    _entityService.ingestAspect(entityUrn2, aspectName, writeAspect2, TEST_AUDIT_STAMP, metadata1);

    // Ingest CorpUserInfo Aspect #3
    CorpUserInfo writeAspect3 = createCorpUserInfo("email3@test.com");
    _entityService.ingestAspect(entityUrn3, aspectName, writeAspect3, TEST_AUDIT_STAMP, metadata1);

    ////
    _step.execute();
    ////

    Map<Urn, List<RecordTemplate>> records = _entityService.getLatestAspects(Set.of(entityUrn1, entityUrn2, entityUrn3), Set.of(aspectName, "dataPlatformInstance"));
    assertEquals(records.size(), 6);
  }

  protected static AuditStamp createTestAuditStamp() {
    try {
      return new AuditStamp().setTime(123L).setActor(Urn.createFromString("urn:li:principal:tester"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to create urn");
    }
  }

  @Nonnull
  protected CorpUserInfo createCorpUserInfo(String email) throws Exception {
    CorpUserInfo corpUserInfo = new CorpUserInfo();
    corpUserInfo.setEmail(email);
    corpUserInfo.setActive(true);
    return corpUserInfo;
  }
}
