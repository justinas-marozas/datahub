package com.linkedin.metadata.entity;

import com.datastax.oss.driver.api.core.CqlSession;
import com.linkedin.metadata.CassandraTestUtils;
import com.linkedin.metadata.entity.cassandra.CassandraAspectDao;
import com.linkedin.metadata.entity.cassandra.CassandraRetentionService;
import com.linkedin.metadata.event.EventProducer;
import com.linkedin.metadata.models.registry.EntityRegistryException;
import java.sql.Timestamp;
import java.time.Instant;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.shaded.org.apache.commons.lang.SerializationUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class CassandraAspectDaoTest {

  private CassandraContainer _cassandraContainer;

  CassandraAspectDao _aspectDao;

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
  }

  @Test
  public void testIt() throws Exception {
    EntityAspect u1a1 = new EntityAspect(
        "urn1",
        "aspect1",
        0L,
        "{metadata}",
        "{systemMetadata}",
        Timestamp.from(Instant.now()),
        "createdBy",
        "createdFor");
    _aspectDao.saveAspect(u1a1, true);
    u1a1.setVersion(1L);
    _aspectDao.saveAspect(u1a1, true);
    EntityAspect u2a1 = new EntityAspect(
        "urn2",
        "aspect1",
        0L,
        "{metadata}",
        "{systemMetadata}",
        Timestamp.from(Instant.now()),
        "createdBy",
        "createdFor");
    _aspectDao.saveAspect(u2a1, true);

    ////
//    _step.
  }
}
