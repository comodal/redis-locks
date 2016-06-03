package com.fabahaba.redis.modules.locks;

import java.util.UUID;

import com.fabahaba.jedipus.cluster.Node;
import com.fabahaba.jedipus.cmds.Cmds;
import com.fabahaba.jedipus.executor.RedisClientExecutor;
import com.fabahaba.redis.locks.AcquireReply;

public final class ReadMeExample {

  private ReadMeExample() {}

  public static void main(final String[] args) {
    cmodule();
  }

  static void cmodule() {
    final String lockName = "MY_LOCK";
    final String ownerId = UUID.randomUUID().toString();
    final String pexpire = "2000";

    try (final RedisClientExecutor rce =
        RedisClientExecutor.startBuilding().createPooled(() -> Node.create("localhost", 6379))) {

      rce.accept(
          client -> client.sendCmd(Cmds.MODULE, Cmds.MODULE_LOAD, "/redis/modules/locks.so"));

      final AcquireReply acquireReply = rce
          .apply(client -> client.sendCmd(RedisMutex.TRY_ACQUIRE, lockName, ownerId, pexpire), 1);

      System.out.format("'%s' has lock '%s' for %dms.%n", acquireReply.getCurrentOwner(), lockName,
          acquireReply.getTTLMillis());

      final String releaseReply =
          rce.apply(client -> client.sendCmd(RedisMutex.TRY_RELEASE, lockName, ownerId), 0);

      if (releaseReply != null && releaseReply.equals(ownerId)) {
        System.out.format("Lock was released by '%s'.%n", ownerId);
      } else {
        System.out.format("Lock was no longer owned by '%s'.%n", ownerId);
      }
    }
  }
}
