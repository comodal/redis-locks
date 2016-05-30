package com.fabahaba.redis.locks.modules;

import com.fabahaba.jedipus.cmds.Cmd;
import com.fabahaba.redis.locks.AcquireReply;

public class RedisMutex {

  public static final Cmd<AcquireReply> TRY_ACQUIRE =
      Cmd.create("LOCKS.MUTEX.TRY.ACQUIRE", AcquireReply.ACQUIRE_REPLY_ADAPTER);
  public static final Cmd<Object[]> TRY_ACQUIRE_RAW = Cmd.createCast("LOCKS.MUTEX.TRY.ACQUIRE");

  public static final Cmd<String> TRY_RELEASE = Cmd.createStringReply("LOCKS.MUTEX.TRY.RELEASE");
  public static final Cmd<byte[]> TRY_RELEASE_RAW = Cmd.createCast("LOCKS.MUTEX.TRY.RELEASE");

}
