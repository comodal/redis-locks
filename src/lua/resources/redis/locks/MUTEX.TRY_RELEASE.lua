-- Returns the current owner at the time of this call.
-- The 'lockName' key is deleted if the requesting owner matches the current.

local lockName = KEYS[1];
local lockOwner = ARGV[1];

local currentOwner = redis.call('get', lockName);

if lockOwner == currentOwner then
   redis.call('del', lockName);
end

return currentOwner;
