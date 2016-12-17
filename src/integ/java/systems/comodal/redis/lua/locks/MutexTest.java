package systems.comodal.redis.lua.locks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import com.fabahaba.jedipus.cluster.Node;
import com.fabahaba.jedipus.cluster.RedisClusterExecutor;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fabahaba.jedipus.client.RedisClient;
import com.fabahaba.jedipus.cmds.CmdByteArray;
import com.fabahaba.jedipus.cmds.RESP;

import systems.comodal.redis.client.BaseRedisClientTest;
import systems.comodal.redis.locks.AcquireReply;

public class MutexTest extends BaseRedisClientTest {

  @BeforeClass
  public static void beforeClass() {
    try (final RedisClient client = DEFAULT_CLIENT_FACTORY_BUILDER.create(DEFAULT_NODE)) {
      RedisMutex.loadMissingScripts(client);
    }
  }

  @Test
  public void testClusterLoad() {
    try (final RedisClusterExecutor rce =
             RedisClusterExecutor.startBuilding(Node.create("localhost", 7379)).create()) {
      RedisMutex.loadMissingScripts(rce);
    }
  }

  @Test
  public void testMutex() throws InterruptedException {
    final String lockName = "lockname";
    final String ownerId = UUID.randomUUID().toString();
    final long pexpire = 2000;
    final CmdByteArray<AcquireReply> acquireCmd =
        RedisMutex.createDirectAcquireArgs(lockName, ownerId, pexpire);
    final CmdByteArray<String> releaseCmd = RedisMutex.createDirectReleaseArgs(lockName, ownerId);

    final AcquireReply acquireReply = RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd);
    assertNull(acquireReply.getPreviousOwner());
    assertEquals(ownerId, acquireReply.getCurrentOwner());
    assertEquals(pexpire, acquireReply.getTTLMillis());

    final AcquireReply renewedOwners = RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd);
    assertEquals(ownerId, renewedOwners.getPreviousOwner());
    assertEquals(ownerId, renewedOwners.getCurrentOwner());
    assertEquals(pexpire, renewedOwners.getTTLMillis());

    Thread.sleep(1);
    final AcquireReply existingOwners = client.sendCmd(RedisMutex.EVALSHA_ACQUIRE,
        RedisMutex.TRY_ACQUIRE.getSha1Hex(), "1", lockName, "nottheowner", "2000");
    assertEquals(ownerId, existingOwners.getPreviousOwner());
    assertEquals(ownerId, existingOwners.getCurrentOwner());
    assertTrue(pexpire > existingOwners.getTTLMillis());

    final String releasedOwner = RedisMutex.TRY_RELEASE.eval(client, releaseCmd);
    assertEquals(ownerId, releasedOwner);
  }

  @Test
  public void testRawMutex() throws InterruptedException {
    final byte[] lockName = RESP.toBytes("lockname");
    final byte[] ownerId = RESP.toBytes(UUID.randomUUID().toString());
    final long pexpire = 2000;
    final CmdByteArray<Object[]> acquireCmd =
        RedisMutex.createDirectAcquireArgs(lockName, ownerId, RESP.toBytes(pexpire));
    final CmdByteArray<byte[]> releaseCmd =
        RedisMutex.createDirectReleaseArgs(lockName, ownerId);

    final Object[] acquireReply = RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd);
    assertNull(acquireReply[0]);
    assertArrayEquals(ownerId, (byte[]) acquireReply[1]);
    assertEquals(pexpire, RESP.longValue(acquireReply[2]));

    final Object[] renewedAcquireReply = RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd);
    assertArrayEquals(ownerId, (byte[]) renewedAcquireReply[0]);
    assertArrayEquals(ownerId, (byte[]) renewedAcquireReply[1]);
    assertEquals(pexpire, RESP.longValue(renewedAcquireReply[2]));

    Thread.sleep(1);
    final Object[] existingOwners =
        client.sendCmd(RedisMutex.EVALSHA_ACQUIRE_RAW, RedisMutex.TRY_ACQUIRE.getSha1HexBytes(),
            RESP.toBytes(1), lockName, RESP.toBytes("nottheowner"), RESP.toBytes(pexpire));
    assertArrayEquals(ownerId, (byte[]) existingOwners[0]);
    assertArrayEquals(ownerId, (byte[]) existingOwners[1]);
    assertTrue(pexpire > RESP.longValue(existingOwners[2]));

    final byte[] releasedOwner = RedisMutex.TRY_RELEASE.eval(client, releaseCmd);
    assertArrayEquals(ownerId, releasedOwner);
  }
}
