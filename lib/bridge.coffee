module.exports =
  send: (args...) ->
    quark.onion.inface.send(args...)
