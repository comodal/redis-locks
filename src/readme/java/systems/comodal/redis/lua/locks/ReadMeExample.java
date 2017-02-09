package systems.comodal.redis.lua.locks;

import com.fabahaba.jedipus.cluster.Node;
import com.fabahaba.jedipus.cluster.RedisClusterExecutor;
import com.fabahaba.jedipus.cluster.RedisClusterExecutor.ReadMode;
import com.fabahaba.jedipus.cmds.CmdByteArray;
import java.util.UUID;
import systems.comodal.redis.locks.AcquireReply;

public final class ReadMeExample {

  private ReadMeExample() {
  }

  public static void main(final String[] args) {
    lua();
  }

  static void lua() {
    final String lockName = "MY_LOCK";
    final String ownerId = UUID.randomUUID().toString();
    final long pexpire = 2000;

    final CmdByteArray<AcquireReply> acquireCmd =
        RedisMutex.createDirectAcquireArgs(lockName, ownerId, pexpire);

    final CmdByteArray<String> releaseCmd = RedisMutex.createDirectReleaseArgs(lockName, ownerId);

    try (final RedisClusterExecutor rce =
        RedisClusterExecutor.startBuilding(Node.create("localhost", 7000)).create()) {

      RedisMutex.loadMissingScripts(rce);

      final AcquireReply acquireReply = rce.apply(ReadMode.MASTER, acquireCmd.getSlot(),
          client -> RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd), 1);

      System.out.format("'%s' has lock '%s' for %dms.%n", acquireReply.getCurrentOwner(), lockName,
          acquireReply.getTTLMillis());

      final String releaseReply = rce.apply(ReadMode.MASTER, acquireCmd.getSlot(),
          client -> RedisMutex.TRY_RELEASE.eval(client, releaseCmd), 0);

      if (releaseReply != null && releaseReply.equals(ownerId)) {
        System.out.format("Lock was released by '%s'.%n", ownerId);
      } else {
        System.out.format("Lock was no longer owned by '%s'.%n", ownerId);
      }
    }
  }
}
