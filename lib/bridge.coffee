module.exports =
  send: (args...) ->
    quark.onion.atom.send(args...)
