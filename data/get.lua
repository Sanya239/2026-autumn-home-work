local counter = 0
local key_count = 5239

request = function()
  counter = counter % key_count + 1
  local path = "/v0/entity?id=load-" .. counter .. "&ack=3"
  return wrk.format("GET", path)
end
