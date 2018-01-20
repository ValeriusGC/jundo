# jundo - Java's undo library

[![Build Status](https://travis-ci.org/ValeriusGC/jundo.svg?branch=master)](https://travis-ci.org/ValeriusGC/jundo)

- [Features](#features)
- [How to use - simple case](#Howto_simple_case)
- [Terms and definitions](#terms-and-definitions)
- [Classes](#classes)
- [Rules and constraints](#rules-and-constraints)
- [Advanced using](#advanced-using)

![gif](https://github.com/ValeriusGC/jundo/blob/master/doc/sample.gif)

## Features

Along with ordinary undo/redo actions this library can:

- keeps command history to use in another place another time
- provides versioning of its subject to make migration possible and easy
- uses 'local context' idiom to play saved commands in another address environment
- allows macros creation to use them later
- uses 'clean state' idiom as point of saving (e.g. to disk) for quick return to it
- has 'merge' option for chain of identical commands (e.g. resizing or moving)
- allows to handle special events for manual tune storing/restoring process if necessary


<a name="Howto_simple_case"></a>
## How to use - simple case

#### create and use

```java
SomeClass ntc = new SomeClass();
UndoStack stack = new UndoStack(ntc, null); 
stack.setWatcher(new SimpleUndoWatcher()); // Watching stack events (optional)
stack.push(new SomeClass.AddCommand(stack, CIRCLE, ntc, null));
stack.push(new SomeClass.AddCommand(stack, RECT, ntc, null));
stack.undo();
stack.redo();
stack.push(new NonTrivialClass.MovedCommand(stack, item, oldPos, null));
stack.undo();
stack.redo();
stack.push(new NonTrivialClass.DeleteCommand(stack, ntc, null));
stack.undo();
stack.undo();
stack.redo();
```

#### save command stack

```java
String store = UndoPacket
    .make(stack, "some.SomeClass", 1) // use id and version (good practice)
    .zipped(true) // packing (optional)
    .onStore(-> Utils::store) // manual tune for subject storing (required for non-serializable subject) 
    .store(); // terminal method for the storing (required)
```

#### restore command stack somewhere

```java
UndoStack stackBack = UndoPacket
    .peek(store, null) // step for pre-check whether stack correct with optional event handler
    .restore(null) // restoring process with optional event handler (for non-serializable stack)
    .stack(null); // return stack final with optional tune via event handler

// That's all. Use stack again as usual
stack.undo();
stack.redo();
stack.push(new NonTrivialClass.MovedCommand(stack, item, oldPos, null));
stack.undo();
stack.redo();
```

#### make macro

Macro is a strictly defined sequence of commands to automate complex change.
Main charachteristic of it to reuse macro later many times.
For example macro
```java
stack.beginMacro("new line");
AddLineCmd(stack, "add line", null)
AddSymbolCmd(stack, "add char", ":", null)
AddSymbolCmd(stack, "add char", "~", null)
AddSymbolCmd(stack, "add char", "$", null)
AddSymbolCmd(stack, "add char", " ", null)
stack.endMacro();
```
which we will use somewhere later should create new line and print the specified characters:

```java
// before macro
:~$ String 1
:~$ String 2
:~$ String 3|

// apply macro
stack.push(some_macro);

// now
:~$ String 1
:~$ String 2
:~$ String 3
:~$ |
```

`UndoStackTest` has method named `testRealMacros()` with macro example

```java
    ...

    // Make macro
    stack.beginMacro("macro 1");
    stack.push(new TextSampleCommands.AddString(stack, "new string", "Hello", null));
    stack.push(new TextSampleCommands.AddString(stack, "new string", ", ", null));
    stack.push(new TextSampleCommands.AddString(stack, "new string", "world!", null));
    stack.endMacro();
    //  Now stack has macro which prints "Hello, world!"
    ...

    // Somewhere in time clone and use it
    UndoCommand macro = stack.clone(stack.getMacros().get(0));
    stack.push(macro); // "Hello, world!" will be printed
    stack.undo(); // "Hello, world!" will be removed
    stack.redo(); // "Hello, world!" will be printed again

   ...

}
```
Macros are stored with stack too.


Of course, advanced using requires more complex approach. It will be explained later. Take a look at some theory.

## Terms and definitions

**JUndo library** is an implementation of the `Command pattern`, for implementing undo/redo functionality in applications.

The `Command pattern` is based on the idea that all editing in an application is done by creating instances of commands. Commands apply changes to the subject and are stored on a command stack. Furthermore, each command knows how to undo its changes to bring the subject back to its previous state. As long as the application only uses commands to change the state of the subject, it is possible to undo a sequence of commands by traversing the stack downwards and calling undo on each command in turn. It is also possible to redo a sequence of commands by traversing the stack upwards and calling redo on each command.

The `subject` in this context, identifies an object whose state can be changed using the commands.

The `undo stack` is the entire list of commands for one subject.

## Classes

- The `UndoCommand` class is the base class of all commands stored on an `UndoStack`
- The `UndoStack` class keeps the entire command chain for subject. Main characteristic of it is that two different stacks should not share one subject
- The `UndoGroup` class is a group of stack instances. An application often has multiple undo stacks, one for each subject. At the same time, an application usually has one undo action and one redo action, which triggers undo or redo for the active subject. UndoGroup is a group of stacks, one of which may be active at the same time
- The `UndoPacket` class controls storing and restoring UndoStack's instances. It has features to tune these processes for various types of subject
- The `UndoWatcher` is an interface to connect to the stack's events

**Additionally:**

- The `RefCmd<V>`: handy generic class to create command for simple cases without creating additional classes
- The `Getter<V>`: to realize 'getter-param' in the `RefCmd<V>`
- The `Setter<V>`: to realize 'setter-param' in the `RefCmd<V>`

## Rules and constraints

#### One subject - one stack

Main characteristic of `undo stack` is that two different stacks should not share one subject. Otherwise very probable collisions and even crash the app.

#### All subject's property changing only via commands

If commands do not totally control property change - they do not control it at all.

#### Store/restore non-serializable subjects only via OnStore/OnRestore event handlers

If subject does not implements Serializable it should be manually tuned in the `onStore(OnStore handler)` when storing and in the `restore(OnRestore handler)` when restoring.

The fact is under the hood the library uses ObjectOutputStream methods as when storing/restoring so when macros create. And the following restriction follows from here -

#### Do not store non-serializable types in commands fields

There is no way to manually tune them, so it will lead to exception later.
Instead use stack's local contexts (see 'advanced using' below).

#### Use external objects that are part of app's memory only via local contexts

This rule refers to views, widgets, string and other resources, etc. When stack will be restored in another address  environment all these reference most likely will be invalid.

## Advanced using

This example is a part of [JavaFx app](https://github.com/ValeriusGC/jundo_javafx_sample). It illustrates library's advanced features.

As mentioned above, advanced using requires more complex approach.

First af all you should plan the design of your 'undo stack' for specific subject.

#### Step 0. Design...

##### ... for commands

We control properties for `javafx.scene.shape.Circle` instance.

- this class doesn't implement `Serializable` so we do not use it in command's fields. Instead we will store specific controlled properties: `ColorUndo` will store color, `RadiusUndo` will store radius and so on
- commands have caption property that can depends on context (stack can be restored on another locale, for example), so we do not store strings but only string identifiers, and request strings dynamically via local contexts of the stack
- app's widgets `javafx.scene.control.Slider` which change `x`, `y` and `radius` do fire events on every minor changes. But we don't need 100 commands for 100 pixels - only one command for entire change. So we will use commands merging

Here how it looks:

```java
// resId - is a string identifier.
public ColorUndo(@NotNull UndoStack owner, UndoCommand parent, int resId, Color oldV, Color newV) {
    super(owner, parent, resId,
        // Color is not Serializable too, so we convert it to JSON
        FxGson.createWithExtras().toJson(oldV),
        FxGson.createWithExtras().toJson(newV));
    }

@Override
protected void doRedo() {
    // Here how to get local context
    ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);
    Color cl = FxGson.createWithExtras().fromJson(newV, Color.class);
    cp.setValue(cl);
}


@Override
protected void doUndo() {
    // Here how to get local context
    ColorPicker cp = (ColorPicker) owner.getLocalContexts().get(IDS_COLOR_PICKER);
    Color cl = FxGson.createWithExtras().fromJson(oldV, Color.class);
    cp.setValue(cl);
}

@Override
public int id() {
    // Here how to set unique id for merging. 
    // The same for XUndo (return 1002) and YUndo (return 1003).
    return 1001; 
}

@Override
public boolean mergeWith(@NotNull UndoCommand cmd) {
    // Here how to merge for RadiusUndo.
    // The same for XUndo and YUndo.
    if(cmd instanceof RadiusUndo) {
        RadiusUndo ruCmd = (RadiusUndo)cmd;
        newV = ruCmd.newV;
        return true;
    }
    return false;
}

@Override
public String getCaption() {
    // Here how to get local context
    Resources res = (Resources) owner.getLocalContexts().get(IDS_RES);
    return res.getString(resId);
}
```

##### ... for undo stack

Widgets and resources are parts of Scene and obviously depend on local memory addressing. So we will use them as local contexts.


#### Step 1. Do instance of the the stack and set the events watcher


```java
stack = new UndoStack(tab.shape, null);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V1());
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);

stack.setWatcher(this);
```
#### Step 2. Commands and stack linking

We use widget and stack events.

```java
//  Link create commands to the events of property
tab.shape.fillProperty().addListener(
    (observable, oldValue, newValue)
        -> stack.push(new BaseTab.UndoBulk.ColorUndo(
            stack, null, 0, (Color)oldValue, (Color)newValue)
));

//  Link stack methods to the app actions
tab.undoBtn.setOnAction(event -> stack.undo());
tab.redoBtn.setOnAction(event -> stack.redo());
tab.saveBtn.setOnAction(event -> stack.setClean());

// Handler of one of stack events
@Override
public void indexChanged(int idx) {
    tab.undoBtn.setDisable(!stack.canUndo());
    tab.redoBtn.setDisable(!stack.canRedo());
    tab.saveBtn.setDisable(stack.isClean());
    tab.undoBtn.setText("undo: " + stack.undoCaption());
    tab.redoBtn.setText("redo: " + stack.redoCaption());
}
```

#### Step 3. Save the stack

Here demonstrates how to work with the non-serializable subject. We just save specific values in the map.
**Very important question: For what we should save subject's state? The fact is the stack has history of changes from start till 'that point of time'. And in new place we should refresh that subject exactly to 'that point of time'.**

```java
private void serialize() throws IOException {
    try {
        String store = UndoPacket
            .make(stack, IDS_STACK, 1)
            .onStore(new UndoPacket.OnStore() {
                @Override
                public Serializable handle(Object subj) {
                    Map<String, Object> props = new HashMap<>();
                        Gson fxGson = FxGson.createWithExtras();
                        props.put("color", FxGson.createWithExtras().toJson(tab.shape.getFill()));
                        props.put("radius", FxGson.createWithExtras().toJson(tab.shape.getRadius()));
                        props.put("x", FxGson.createWithExtras().toJson(tab.shape.getCenterX()));
                        props.put("y", FxGson.createWithExtras().toJson(tab.shape.getCenterY()));
                        return fxGson.toJson(props);
                    }
            })
            .zipped(true)
            .store();

        // Simply store in file
        Files.write(Paths.get("./undo.txt"), store.getBytes());
    } catch (Exception e) {
        System.err.println(e.getLocalizedMessage());
    }
}
```

#### Step 4. Restore the stack in another time another place. Continue using as usual

**See, that we not only restore stack but migrate our subject's properties to the new version of it!**

```java
// Get string
String store = new String(Files.readAllBytes(Paths.get("./undo.txt")));

stack = UndoPacket
        // Check whether we got appropriate stack
        .peek(store, subjInfo -> IDS_STACK.equals(subjInfo.id))
        // Manual restoring (because we store non-serializable type)
        .restore((processedSubj, subjInfo) -> {
            // First, manual tune for restoring types from string
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            HashMap<String, Object> map = new Gson().fromJson((String) processedSubj, type);
            if(subjInfo.version == 1) {
                // Second - migration from V1 to V2!
                Gson fxGson = FxGson.createWithExtras();
                Color c = fxGson.fromJson(map.get("color").toString(), Color.class);
                tab.colorPicker.setValue(c);
                Double r = fxGson.fromJson(map.get("radius").toString(), Double.class);
                tab.radius.setValue(r);
                Double x = fxGson.fromJson(map.get("x").toString(), Double.class);
                tab.centerX.setValue(x);
                Double y = fxGson.fromJson(map.get("y").toString(), Double.class);
                tab.centerY.setValue(y);
            }
            return map;
        })
        .stack((stack, subjInfo) -> {
            // Restore new local contexts
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RES, new Resources_V2());
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_COLOR_PICKER, tab.colorPicker);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_RADIUS_SLIDER, tab.radius);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_X_SLIDER, tab.centerX);
            stack.getLocalContexts().put(BaseTab.UndoBulk.IDS_Y_SLIDER, tab.centerY);
        });

// Process case when we don't restore stack
if(null == stack)
    stack = new UndoStack(tab.shape, null);
// Restore watcher
stack.setWatcher(this);
```

Next connection to app's widgets and actions - as in **Step 2. Commands and stack linking**.

**Voila!**

As you see if you take time for design you get simple and elegant undo system.

## Download

#### Maven

	<dependency>
	    <groupId>com.gdetotut</groupId>
	    <artifactId>jundo-framework</artifactId>
	    <version>1.15</version>
	</dependency>

#### Gradle

	compile 'com.gdetotut:jundo-framework:1.15'

*[See it on the Maven](https://mvnrepository.com/artifact/com.gdetotut/jundo-framework/1.15)*

- - -

The library has a lot of tests with using techniques. See them in the code.