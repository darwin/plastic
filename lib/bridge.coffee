module.exports =
  system: null

  boot: (args...) ->
    @system = plastic.api.boot(args...)
  send: (args...) ->
    plastic.api.send(@system, args...)
