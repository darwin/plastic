(defn pure [handler]
  (if (map? app-db)
    (warn "re-frame: Looks like \"pure\" is in the middleware pipeline twice. Ignoring.")
    (warn "re-frame: \"pure\" middleware not given a Ratom.  Got: " app-db))
  handler)