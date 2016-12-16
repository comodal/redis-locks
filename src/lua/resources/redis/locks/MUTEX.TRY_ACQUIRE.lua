-- Returns the previous owner, the current owner and the pttl for the lock.
-- Returns either {null, lockOwner, pexpire}, {owner, owner, pexpire} or {owner, owner, pttl}.
-- The previous owner is null if 'lockOwner' newly acquired the lock. Otherwise, the previous
-- owner will be same value as the current owner. If the current owner is equal to the supplied
-- 'lockOwner' argument then the ownership claim will remain active for 'pexpire' milliseconds.

local lockName = KEYS[1];
local lockOwner = ARGV[1];
local owner = redis.call('get', lockName);

if not owner or owner == lockOwner then
   local px = tonumber(ARGV[2]);
   redis.call('set', lockName, lockOwner, 'PX', px);
   return {owner, lockOwner, px};
end

return {owner, owner, redis.call('pttl', lockName)};
