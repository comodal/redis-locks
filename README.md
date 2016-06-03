##Redis-Locks [![Build Status](https://img.shields.io/travis/jamespedwards42/redis-locks.svg?branch=master)](https://travis-ci.org/jamespedwards42/redis-locks) [![Bintray](https://api.bintray.com/packages/jamespedwards42/libs/redis-locks/images/download.svg) ](https://bintray.com/jamespedwards42/libs/redis-locks/_latestVersion) [![license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/jamespedwards42/redis-locks/master/LICENSE)

>Distributed systems locks implemented in both Redis Lua scripts and C modules.  A Java client library is also provided for making calls and loading the locks to Redis.

* If you are only interested in the C modules see the [Modules README](https://github.com/jamespedwards42/redis-locks/tree/master/redis/modules#locks-api-reference).
* If you are only interested in the Lua scripts see [redis-locks/src/lua/resources/redis/locks](src/lua/resources/redis/locks).  They are self documented.
* If you are interested the Java client library, which uses [Jedipus](https://github.com/jamespedwards42/jedipus#jedipus------), continue below:
* If you are interested in distributed Google Guava services built on top of these locks see [redis-distributable-services](https://github.com/jamespedwards42/distributable-services/tree/master/redis#redis-distributable-services------).

######Dependency Management
```groovy
repositories {
   jcenter()
}

dependencies {
   compile 'com.fabahaba:jedipus:+'
   compile 'com.fabahaba:redis-locks:+'
}
```

## MUTEX
The intended usage of this lock is to serve leader elections amongst distributed services.  Be aware that after a failover of your Redis server a new service could claim leadership before your previous leader has realized it has lost leadership.  To protect against this corner case, when the previous owner is null, have your service acquire the lock twice, effectively waiting for two full checkin periods.  This will ensure that the previous leader has attempted to refresh its claim and discovered it is no longer the leader.

#### [Demo Module Usage](src/readme/java/com/fabahaba/redis/modules/locks/ReadMeExample.java)

>Note: The example auto close the `RedisClientExecutor` but you probably want it to be a long lived object.

```java
final String lockName = "MY_LOCK";
final String ownerId = UUID.randomUUID().toString();
final String pexpire = "2000";

try (final RedisClientExecutor rce =
    RedisClientExecutor.startBuilding().createPooled(() -> Node.create("localhost", 6379))) {

  rce.accept(client -> client.sendCmd(Cmds.MODULE, Cmds.MODULE_LOAD, "/redis/modules/locks.so"));

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
```

#### [Demo Lua Usage](src/readme/java/com/fabahaba/redis/lua/locks/ReadMeExample.java)

>Note: The example auto close the `RedisClusterExecutor` but you probably want it to be a long lived object.

```java
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
```