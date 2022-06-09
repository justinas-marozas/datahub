package com.linkedin.metadata.boot;

import com.linkedin.chart.ChartInfo;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.ChangeAuditStamps;
import com.linkedin.common.urn.Urn;
import com.linkedin.identity.CorpUserInfo;
import com.linkedin.metadata.CassandraTestUtils;
import com.linkedin.metadata.EbeanTestUtils;
import com.linkedin.metadata.boot.steps.IngestDataPlatformInstancesStep;
import com.linkedin.metadata.entity.EntityAspect;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.RetentionService;
import com.linkedin.metadata.entity.TestEntityRegistry;
import com.linkedin.metadata.entity.ebean.EbeanAspectDao;
import com.linkedin.metadata.entity.ebean.EbeanRetentionService;
import com.linkedin.metadata.event.EventProducer;
import com.linkedin.metadata.models.registry.ConfigEntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistryException;
import com.linkedin.metadata.models.registry.MergedEntityRegistry;
import com.linkedin.metadata.snapshot.Snapshot;
import com.linkedin.metadata.utils.PegasusUtils;
import com.linkedin.mxe.SystemMetadata;
import io.ebean.EbeanServer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.testcontainers.containers.CassandraContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.linkedin.metadata.Constants.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class EbeanIngestDataPlatformInstancesTest {

  protected static final AuditStamp TEST_AUDIT_STAMP = createTestAuditStamp();
  protected final EntityRegistry _snapshotEntityRegistry;
  protected final EntityRegistry _configEntityRegistry;
  protected final EntityRegistry _testEntityRegistry;
  protected EventProducer _mockProducer;

  private EbeanAspectDao _aspectDao;

  private IngestDataPlatformInstancesStep _step;
  private CassandraContainer _cassandraContainer;
  private EntityService _entityService;
  private RetentionService _retentionService;

  public EbeanIngestDataPlatformInstancesTest() throws EntityRegistryException {
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
    EbeanServer server = EbeanTestUtils.createTestServer();
    _mockProducer = mock(EventProducer.class);
    _aspectDao = new EbeanAspectDao(server);
    _aspectDao.setConnectionValidated(true);
    _entityService = new EntityService(_aspectDao, _mockProducer, _testEntityRegistry);
    _retentionService = new EbeanRetentionService(_entityService, server, 1000);
    _entityService.setRetentionService(_retentionService);
    _step = new IngestDataPlatformInstancesStep(_entityService, _aspectDao);
  }

  @Test
  public void testIt() throws Exception {
    SystemMetadata metadata = new SystemMetadata();
    metadata.setLastObserved(1625792689);
    metadata.setRunId("run-123");

    // Ingest some CorpUserInfo aspects
    // CorpUser has no data platform associated to it, so it should be ignored
    List<Urn> corpUserUrns = ingestThisManyCorpUserInfoAspects(123, metadata);

    // Ingest loads of ChartInfo aspects to make sure that the step needs to run multiple batches
    // Charts are associated with a data platform, so they should receive a data platform instance aspect
    List<Urn> chartUrns = ingestThisManyChartInfoAspects(2222, metadata);

    // Sanity check
    assertFalse(_aspectDao.checkIfAspectExists(DATA_PLATFORM_INSTANCE_ASPECT_NAME));

    _step.execute();

    // Check if the step inserted anything
    assertTrue(_aspectDao.checkIfAspectExists(DATA_PLATFORM_INSTANCE_ASPECT_NAME));
    // Check CorpUser didn't receive a new aspect
    for (Urn urn : corpUserUrns) {
      EntityAspect corpUserAspect = _aspectDao.getLatestAspect(urn.toString(), DATA_PLATFORM_INSTANCE_ASPECT_NAME);
      assertNull(corpUserAspect, urn.toString());
    }
    // Check Charts received a new aspect
    for (Urn urn : chartUrns) {
      EntityAspect chartAspect = _aspectDao.getLatestAspect(urn.toString(), DATA_PLATFORM_INSTANCE_ASPECT_NAME);
      assertNotNull(chartAspect, urn.toString());
    }
  }

  protected static AuditStamp createTestAuditStamp() {
    try {
      return new AuditStamp().setTime(123L).setActor(Urn.createFromString("urn:li:corpuser:tester"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to create urn");
    }
  }

  protected List<Urn> ingestThisManyChartInfoAspects(int n, SystemMetadata metadata) throws URISyntaxException {
    String aspectName = PegasusUtils.getAspectNameFromSchema(new ChartInfo().schema());
    List<Urn> allUrns = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Urn urn = Urn.createFromString(String.format("urn:li:chart:(looker,test%d)", i));
      allUrns.add(urn);
      ChartInfo aspect = createChartInfo(String.format("Test Title %d", i), String.format("Test description %d", i));
      _entityService.ingestAspect(urn, aspectName, aspect, TEST_AUDIT_STAMP, metadata);
    }
    return allUrns;
  }

  @Nonnull
  protected ChartInfo createChartInfo(String title, String description) {
    ChartInfo chartInfo = new ChartInfo();
    chartInfo.setTitle(title);
    chartInfo.setDescription(description);
    ChangeAuditStamps lastModified = new ChangeAuditStamps();
    lastModified.setCreated(createTestAuditStamp());
    lastModified.setLastModified(createTestAuditStamp());
    chartInfo.setLastModified(lastModified);
    return chartInfo;
  }

  protected List<Urn> ingestThisManyCorpUserInfoAspects(int n, SystemMetadata metadata) throws URISyntaxException {
    String aspectName = PegasusUtils.getAspectNameFromSchema(new CorpUserInfo().schema());
    List<Urn> allUrns = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Urn urn = Urn.createFromString(String.format("urn:li:corpuser:tester%d", i));
      allUrns.add(urn);
      CorpUserInfo aspect = createCorpUserInfo(String.format("email%d@test.com", i));
      _entityService.ingestAspect(urn, aspectName, aspect, TEST_AUDIT_STAMP, metadata);
    }
    return allUrns;
  }

  @Nonnull
  protected CorpUserInfo createCorpUserInfo(String email) {
    CorpUserInfo corpUserInfo = new CorpUserInfo();
    corpUserInfo.setEmail(email);
    corpUserInfo.setActive(true);
    return corpUserInfo;
  }
}
