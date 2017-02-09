package systems.comodal.redis.modules.locks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fabahaba.jedipus.client.RedisClient;
import com.fabahaba.jedipus.cmds.Cmds;
import com.fabahaba.jedipus.cmds.RESP;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import systems.comodal.redis.client.BaseRedisClientTest;
import systems.comodal.redis.locks.AcquireReply;

public class MutexTest extends BaseRedisClientTest {

  @BeforeClass
  public static void beforeClass() {
    try (final RedisClient client = DEFAULT_CLIENT_FACTORY_BUILDER.create(DEFAULT_NODE)) {
      client.sendCmd(Cmds.MODULE, Cmds.MODULE_LOAD, "/redis/modules/locks.so");
    }
  }

  @Test
  public void testMutex() throws InterruptedException {
    final String lockName = "lockname";
    final String ownerId = UUID.randomUUID().toString();
    final long pexpire = 2000;

    final AcquireReply acquireReply =
        client.sendCmd(RedisMutex.TRY_ACQUIRE, lockName, ownerId, Long.toString(2000));
    assertNull(acquireReply.getPreviousOwner());
    assertEquals(ownerId, acquireReply.getCurrentOwner());
    assertEquals(pexpire, acquireReply.getTTLMillis());

    final AcquireReply renewedOwners =
        client.sendCmd(RedisMutex.TRY_ACQUIRE, lockName, ownerId, Long.toString(2000));
    assertEquals(ownerId, renewedOwners.getPreviousOwner());
    assertEquals(ownerId, renewedOwners.getCurrentOwner());
    assertEquals(pexpire, renewedOwners.getTTLMillis());

    Thread.sleep(1);
    final AcquireReply existingOwners =
        client.sendCmd(RedisMutex.TRY_ACQUIRE, lockName, "nottheowner", Long.toString(2000));
    assertEquals(ownerId, existingOwners.getPreviousOwner());
    assertEquals(ownerId, existingOwners.getCurrentOwner());
    assertTrue(pexpire > existingOwners.getTTLMillis());

    final String releasedOwner = client.sendCmd(RedisMutex.TRY_RELEASE, lockName, ownerId);
    assertEquals(ownerId, releasedOwner);
  }

  @Test
  public void testRawMutex() throws InterruptedException {
    final byte[] lockName = RESP.toBytes("lockname");
    final byte[] ownerId = RESP.toBytes(UUID.randomUUID().toString());
    final long pexpire = 2000;

    final Object[] acquireReply =
        client.sendCmd(RedisMutex.TRY_ACQUIRE_RAW, lockName, ownerId, RESP.toBytes(pexpire));
    assertNull(acquireReply[0]);
    assertArrayEquals(ownerId, (byte[]) acquireReply[1]);
    assertEquals(pexpire, RESP.longValue(acquireReply[2]));

    final Object[] renewedAcquireReply =
        client.sendCmd(RedisMutex.TRY_ACQUIRE_RAW, lockName, ownerId, RESP.toBytes(pexpire));
    assertArrayEquals(ownerId, (byte[]) renewedAcquireReply[0]);
    assertArrayEquals(ownerId, (byte[]) renewedAcquireReply[1]);
    assertEquals(pexpire, RESP.longValue(renewedAcquireReply[2]));

    Thread.sleep(1);
    final Object[] existingOwners = client.sendCmd(RedisMutex.TRY_ACQUIRE_RAW, lockName,
        RESP.toBytes("nottheowner"), RESP.toBytes(pexpire));
    assertArrayEquals(ownerId, (byte[]) existingOwners[0]);
    assertArrayEquals(ownerId, (byte[]) existingOwners[1]);
    assertTrue(pexpire > RESP.longValue(existingOwners[2]));

    final byte[] releasedOwner = client.sendCmd(RedisMutex.TRY_RELEASE_RAW, lockName, ownerId);
    assertArrayEquals(ownerId, releasedOwner);
  }
}
