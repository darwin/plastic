These steps are for my OS X box. Worked around commit [#0638331e63](https://github.com/darwin/plastic/commit/0638331e63aaed1d78839638219069d24f7818e6). 

All is still in flux. You have to know a bit about node.js and clojurescript ecosystems. I don't cover it here. 

Good luck!

### initial setup

##### prepare workspace

    mkdir workspace
    cd workspace

From now on I will be refering to this directory as `$workspace`.

##### clone atom and plastic

    git clone git@github.com:darwin/plastic.git
    git clone git@github.com:atom/atom.git

##### build Atom

build atom according to [Atom's instructions](https://github.com/atom/atom/tree/master/docs/build-instructions)

    cd atom
    ./script/build
    cd ..

##### launch custom Atom for the first time

    cd plastic/cljs
    ./script/atom.sh

This will run Atom from sources and you should see familiar Atom welcome screen with two tabs side by side.
Make sure Atom works at this point.

This first run should have created atom-profile dir in $workspace/.atom.
This home dir is a brand new atom profile for development.

##### install plastic package under your custom Atom profile

Quit Atom and install plastic package into Atom.

    cd ../../.atom
    mkdir packages
    cd packages
    ln -s ../../plastic plastic-clojurescript

`plastic` symlink should point to $workspace/plastic

##### plastic node dependencies

install plastic npm deps:

    cd ../../plastic
    npm install
    apm rebuild

##### plastic bleeding-edge  dependencies

I might be using bleeding-edge versions of some clojurescript libraries, look into [$workspace/plastic/cljs/project.clj](https://github.com/darwin/plastic/blob/master/cljs/project.clj) and look for uncommented paths into checkouts directory.

Most likely they should point to forks of libraries maintained under [https://github.com/darwin/[lib-name]](https://github.com/darwin?tab=repositories).

For example there might be "checkouts/re-frame/src", so you need to find [https://github.com/darwin/re-frame](https://github.com/darwin/re-frame) repo on github and clone it into checkouts.

    cd cljs
    mkdir checkouts
    cd checkouts
    git clone https://github.com/darwin/re-frame.git

Alternatively, you may try to comment out those bleeding-edge checkouts out, it might work.

##### compile plastic clojurescript files

First run Figwheel and wait for it to finish compilation and enter into waiting mode:

    cd $workspace/plastic/cljs
    ./script/figwheel.sh

Wait for figwheel to finish booting. And then in another terminal session

    cd $workspace/plastic/cljs
    ./script/atom.sh

Atom file browser should point to $workspace/plastic/cljs/src, open some cljs files from `src/playground` and have fun!

Note: effective keyboard shortcuts are specified in [$workspace/plastic/keymaps/plastic.cson](https://github.com/darwin/plastic/blob/master/keymaps/plastic.cson).

##### native modules => apm rebuild

Sometimes Atom needs you to compile some native modules, it will shout at you from the devtools console inside Atom.

    cd $workspace/plastic
    apm rebuild

### normal dev cycle

First run Figwheel and wait for it to finish compilation and enter into waiting mode:

    cd $workspace/plastic/cljs
    ./script/dev.sh

And in another shell session:

    cd $workspace/plastic/cljs
    ./script/atom.sh

Figwheel should do automatic code reloading, Atom itself in devmode watches for css (or .less) changes

Note: sometimes figwheel can get crazy after initial launch and sends a lot of files for re-evaluation. Killing atom and launching it again always helped in this case.

### dev tips

##### open devtools on launch

I use this `$workspace/.atom/init.coffee` script to open devtools console at launch:

    toggleDevtools = ->
      if workspaceElement = atom.views.getView(atom.workspace)
        atom.commands.dispatch workspaceElement, "window:toggle-dev-tools"

    setTimeout toggleDevtools, 1000

##### cljs-devtools integration

Plastic uses cljs-devtools heavily. You should enable it in your Atom devtools console. follow [this article](https://github.com/binaryage/cljs-devtools#enable-custom-formatters-in-your-chrome-canary)

This setting should persist, so next time you kill Atom and launch it again, cljs-devtools should be enabled from the beginning.

#### running with web workers

For development it is easier to run all code on main thread. That is default behaviour for now.
In release mode Plastic will be able to utilize multiple threads thanks to web workers. You can run Plastic using web workers by following steps below.
Warning: This is experimental and could be broken from time to time because I spend most time running dev.sh. 
         But general idea is to have editor heavy-lifting on worker thread and main thread available for Atom and UI updates only.

First run Figwheel and wait for it to finish compilation and enter into waiting mode:

    cd $workspace/plastic/cljs
    ./script/run.sh

And in another shell session:

    cd $workspace/plastic/cljs
    ./script/atom.sh

## happy hacking!
