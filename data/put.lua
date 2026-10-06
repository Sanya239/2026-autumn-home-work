local counter = 0
local headers = {
  ["Content-Type"] = "application/octet-stream"
}

request = function()
  counter = counter + 1
  local path = "/v0/entity?id=load-" .. counter
  local body = "value-" .. counter
  return wrk.format("PUT", path, headers, body)
end

