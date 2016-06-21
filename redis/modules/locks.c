#include "../redismodule.h"
#include <string.h>

int MutexTryAcquire_RedisCommand(RedisModuleCtx *ctx, RedisModuleString **argv, int argc) {
  if (argc != 4) {
    return RedisModule_WrongArity(ctx);
  }

  RedisModuleKey *lockKey = RedisModule_OpenKey(ctx, argv[1], REDISMODULE_READ|REDISMODULE_WRITE);

  int keyType = RedisModule_KeyType(lockKey);
  if (keyType != REDISMODULE_KEYTYPE_STRING && keyType != REDISMODULE_KEYTYPE_EMPTY) {
    RedisModule_CloseKey(lockKey);
    return RedisModule_ReplyWithError(ctx, REDISMODULE_ERRORMSG_WRONGTYPE);
  }

  RedisModuleString *requestingOwner = argv[2];
  RedisModule_ReplyWithArray(ctx, 3);

  if (keyType == REDISMODULE_KEYTYPE_EMPTY) {
    mstime_t px;
    if (RedisModule_StringToLongLong(argv[3], &px) != REDISMODULE_OK) {
      RedisModule_CloseKey(lockKey);
      return RedisModule_ReplyWithError(ctx, "ERR invalid pexire");
    }

    RedisModule_StringSet(lockKey, requestingOwner);
    RedisModule_SetExpire(lockKey, px);

    RedisModule_ReplyWithNull(ctx);
    RedisModule_ReplyWithString(ctx, requestingOwner);
    RedisModule_ReplyWithLongLong(ctx, px);

    RedisModule_CloseKey(lockKey);
    return REDISMODULE_OK;
  }

  size_t currentOwnerLen, requestingOwnerLen;
  const char *currentOwnerStringPtr = RedisModule_StringDMA(lockKey, &currentOwnerLen, REDISMODULE_READ);
  const char *requestingOwnerStringPtr = RedisModule_StringPtrLen(requestingOwner, &requestingOwnerLen);

  if (currentOwnerLen == requestingOwnerLen && memcmp(currentOwnerStringPtr, requestingOwnerStringPtr, currentOwnerLen) == 0) {
    mstime_t px;
    if (RedisModule_StringToLongLong(argv[3], &px) != REDISMODULE_OK) {
      RedisModule_CloseKey(lockKey);
      return RedisModule_ReplyWithError(ctx, "ERR invalid pexire");
    }

    RedisModule_SetExpire(lockKey, px);

    RedisModule_ReplyWithString(ctx, requestingOwner);
    RedisModule_ReplyWithString(ctx, requestingOwner);
    RedisModule_ReplyWithLongLong(ctx, px);

    RedisModule_CloseKey(lockKey);
    return REDISMODULE_OK;
  }

  RedisModule_ReplyWithStringBuffer(ctx, currentOwnerStringPtr, currentOwnerLen);
  RedisModule_ReplyWithStringBuffer(ctx, currentOwnerStringPtr, currentOwnerLen);
  RedisModule_ReplyWithLongLong(ctx, RedisModule_GetExpire(lockKey));

  RedisModule_CloseKey(lockKey);
  return REDISMODULE_OK;
}

int MutexTryRelease_RedisCommand(RedisModuleCtx *ctx, RedisModuleString **argv, int argc) {
  if (argc != 3) {
    return RedisModule_WrongArity(ctx);
  }

  RedisModuleKey *lockKey = RedisModule_OpenKey(ctx, argv[1], REDISMODULE_READ|REDISMODULE_WRITE);

  int keyType = RedisModule_KeyType(lockKey);
  if (keyType == REDISMODULE_KEYTYPE_EMPTY) {
    RedisModule_CloseKey(lockKey);
    RedisModule_ReplyWithNull(ctx);
    return REDISMODULE_OK;
  }

  if (keyType != REDISMODULE_KEYTYPE_STRING) {
    RedisModule_CloseKey(lockKey);
    return RedisModule_ReplyWithError(ctx, REDISMODULE_ERRORMSG_WRONGTYPE);
  }

  RedisModuleString *requestingOwner = argv[2];
  size_t currentOwnerLen, requestingOwnerLen;
  const char *currentOwnerStringPtr = RedisModule_StringDMA(lockKey, &currentOwnerLen, REDISMODULE_READ);
  const char *requestingOwnerStringPtr = RedisModule_StringPtrLen(requestingOwner, &requestingOwnerLen);

  if (currentOwnerLen == requestingOwnerLen && memcmp(currentOwnerStringPtr, requestingOwnerStringPtr, currentOwnerLen) == 0) {
    RedisModule_DeleteKey(lockKey);
    RedisModule_ReplyWithString(ctx, requestingOwner);
  } else {
    RedisModule_ReplyWithStringBuffer(ctx, currentOwnerStringPtr, currentOwnerLen);
  }

  RedisModule_CloseKey(lockKey);
  return REDISMODULE_OK;
}

int RedisModule_OnLoad(RedisModuleCtx *ctx, RedisModuleString **argv, int argc) {
   if (RedisModule_Init(ctx, "locks", 1, REDISMODULE_APIVER_1) == REDISMODULE_ERR) {
     return REDISMODULE_ERR;
   }
   if (RedisModule_CreateCommand(ctx, "locks.mutex.try.acquire", MutexTryAcquire_RedisCommand, "write deny-oom fast", 1, 1, 1) == REDISMODULE_ERR) {
     return REDISMODULE_ERR;
   }
   if (RedisModule_CreateCommand(ctx, "locks.mutex.try.release", MutexTryRelease_RedisCommand, "write fast", 1, 1, 1) == REDISMODULE_ERR) {
     return REDISMODULE_ERR;
   }
   return REDISMODULE_OK;
}
