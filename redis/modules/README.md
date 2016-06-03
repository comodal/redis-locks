# Locks API reference

## `LOCKS.MUTEX.TRY.ACQUIRE lockName ownerId pexpire`

Tries to acquire the lock if it is not currently owned by anyone else.  If the lock is newly acquired or `ownerId` matches the current owner the expiration will be set to `pexpire` milliseconds.

#### Return value
[Array reply](http://redis.io/topics/protocol#array-reply):  The previous owner (possibly null), the current owner and the time to live in milliseconds.

####Example
redis> LOCKS.MUTEX.TRY.ACQUIRE myLock myId 1000

1. null
2. "myId"
3. 1000

redis> LOCKS.MUTEX.TRY.ACQUIRE myLock myId 1000

1. "myId"
2. "myId"
3. 1000

redis> LOCKS.MUTEX.TRY.ACQUIRE myLock someOtherId 1000

1. "myId"
2. "myId"
3. 999

## `LOCKS.MUTEX.TRY.RELEASE lockName ownerId`

Frees the lock if it is currently held by `ownerId`.

#### Return value
[Bulk string reply](http://redis.io/topics/protocol#bulk-string-reply):  The owner at the time of this call.

####Example
redis> LOCKS.MUTEX.TRY.TRY_RELEASE myLock myId

null

redis> LOCKS.MUTEX.TRY.ACQUIRE myLock myId 2000

1. null
2. "myId"
3. 2000

redis> LOCKS.MUTEX.TRY.TRY_RELEASE myLock myId

"myId"
