require("./_build/main/plastic.js") # this requires just plastic.env

goog.require("plastic.env")

# require this as soon as possible and boot plastic with delay
# see plastic.coffee, activate/bootPlastic method
goog.require("plastic.dev")