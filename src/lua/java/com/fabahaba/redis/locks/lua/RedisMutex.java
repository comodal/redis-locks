package com.fabahaba.redis.locks.lua;

import java.util.UUID;

import com.fabahaba.jedipus.client.RedisClient;
import com.fabahaba.jedipus.cluster.Node;
import com.fabahaba.jedipus.cluster.RedisClusterExecutor;
import com.fabahaba.jedipus.cluster.RedisClusterExecutor.ReadMode;
import com.fabahaba.jedipus.cmds.Cmd;
import com.fabahaba.jedipus.cmds.CmdByteArray;
import com.fabahaba.jedipus.cmds.RESP;
import com.fabahaba.jedipus.lua.LuaScript;
import com.fabahaba.redis.locks.AcquireReply;

public final class RedisMutex {

  private RedisMutex() {}

  public static final LuaScript TRY_ACQUIRE =
      LuaScript.fromResourcePath("/redis/locks/MUTEX.TRY_ACQUIRE.lua");

  public static final Cmd<AcquireReply> EVALSHA_ACQUIRE =
      Cmd.create("EVALSHA", AcquireReply.ACQUIRE_REPLY_ADAPTER);

  public static final Cmd<Object[]> EVALSHA_ACQUIRE_RAW = Cmd.createCast("EVALSHA");

  public static final LuaScript TRY_RELEASE =
      LuaScript.fromResourcePath("/redis/locks/MUTEX.TRY_RELEASE.lua");

  public static final Cmd<String> EVALSHA_RELEASE = Cmd.createStringReply("EVALSHA");
  public static final Cmd<byte[]> EVALSHA_RELEASE_RAW = Cmd.createCast("EVALSHA");

  public static void loadMissingScripts(final RedisClusterExecutor rce) {
    LuaScript.loadMissingScripts(rce, TRY_ACQUIRE, TRY_RELEASE);
  }

  public static void loadMissingScripts(final RedisClient client) {
    LuaScript.loadMissingScripts(client, TRY_ACQUIRE, TRY_RELEASE);
  }

  private static final byte[] NUM_KEYS = RESP.toBytes(1);

  public static CmdByteArray<AcquireReply> createDirectAcquireArgs(final String lockName,
      final String ownerId, final long pexpire) {

    return CmdByteArray.startBuilding(EVALSHA_ACQUIRE, 6).addArg(TRY_ACQUIRE.getSha1HexBytes())
        .addArg(NUM_KEYS).addSlotKey(lockName).addArg(ownerId).addArg(pexpire).create();
  }

  public static CmdByteArray<Object[]> createDirectAcquireArgs(final byte[] lockName,
      final byte[] ownerId, final byte[] pexpire) {

    return CmdByteArray.startBuilding(EVALSHA_ACQUIRE_RAW, 6).addArg(TRY_ACQUIRE.getSha1HexBytes())
        .addArg(NUM_KEYS).addSlotKey(lockName).addArg(ownerId).addArg(pexpire).create();
  }

  public static CmdByteArray<String> createDirectReleaseArgs(final String lockName,
      final String ownerId) {

    return CmdByteArray.startBuilding(EVALSHA_RELEASE, 5).addArg(TRY_RELEASE.getSha1HexBytes())
        .addArg(NUM_KEYS).addSlotKey(lockName).addArg(ownerId).create();
  }

  public static CmdByteArray<byte[]> createDirectReleaseArgs(final byte[] lockName,
      final byte[] ownerId) {

    return CmdByteArray.startBuilding(EVALSHA_RELEASE_RAW, 5).addArg(TRY_RELEASE.getSha1HexBytes())
        .addArg(NUM_KEYS).addSlotKey(lockName).addArg(ownerId).create();
  }

  public static void main(final String[] args) {
    readme();
  }

  static void readme() {
    final String lockName = "MY_LOCK";
    final String ownerId = UUID.randomUUID().toString();
    final long pexpire = 2000;

    final CmdByteArray<AcquireReply> acquireCmd =
        RedisMutex.createDirectAcquireArgs(lockName, ownerId, pexpire);

    final CmdByteArray<String> releaseCmd = RedisMutex.createDirectReleaseArgs(lockName, ownerId);

    try (final RedisClusterExecutor rce =
        RedisClusterExecutor.startBuilding(Node.create("localhost", 7000)).create()) {

      RedisMutex.loadMissingScripts(rce);

      for (;;) {
        final AcquireReply lockOwners = rce.apply(ReadMode.MASTER, acquireCmd.getSlot(),
            client -> RedisMutex.TRY_ACQUIRE.eval(client, acquireCmd), 1);

        if (lockOwners.getCurrentOwner().equals(ownerId)) {
          try {
            System.out.format("Lock '%s' acquired by '%s'.%n", lockName, ownerId);
            break;
          } finally {
            // Even if tryReleaseLock fails for any reason (network errors) the lock will expire
            // after pexpire has elapsed. null is returned if the lock was no longer owned.
            final String released = rce.apply(ReadMode.MASTER, acquireCmd.getSlot(),
                client -> RedisMutex.TRY_RELEASE.eval(client, releaseCmd), 0);

            if (released != null && released.equals(ownerId)) {
              System.out.format("Lock '%s' was released by '%s'.%n", lockName, ownerId);
            } else {
              System.out.format("Lock '%s' was no longer owned by '%s'.%n", lockName, ownerId);
            }
          }
        }

        System.out.format("Waiting for %dms before retrying.", lockOwners.getTTLMillis());
        Thread.sleep(lockOwners.getTTLMillis());
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
