# jundo (Java's Undo Framework)
Java undo/redo framework

[![Build Status](https://travis-ci.org/ValeriusGC/jundo.svg?branch=master)](https://travis-ci.org/ValeriusGC/jundo)

## Summary

**Java's Undo Framework** is an implementation of the `Command pattern`, for implementing undo/redo functionality in applications.

The `Command pattern` is based on the idea that all editing in an application is done by creating instances of command objects. Command objects apply changes to the document and are stored on a command stack. Furthermore, each command knows how to undo its changes to bring the document back to its previous state. As long as the application only uses command objects to change the state of the document, it is possible to undo a sequence of commands by traversing the stack downwards and calling undo on each command in turn. It is also possible to redo a sequence of commands by traversing the stack upwards and calling redo on each command.

**Java's Undo Framework** is inspired by the implementation of the [Qt's Undo Framework](http://doc.qt.io/qt-5/qundo.html), with some changes and additions. 

So, a template implementation of UndoCommand was added, which allows creating a command for simply changing a property without creating a class (FunctionalCommand <V extends java.io.Serializable>).

 ```java
// How FunctionalCommand works
Point pt = new Point(-30, -40);
UndoStack stack = new UndoStack(pt, null);
UndoCommand undoCommand = new UndoCommand("Move point", null);
new FunctionalCommand<>("Change x", pt::getX, pt::setX, 10, undoCommand);
new FunctionalCommand<>("Change y", pt::getY, pt::setY, 20, undoCommand);
stack.push(undoCommand);
```

Besides serialization mechanism was added, so one can save all undo/redo chains to Base64 string and deserialize them later to objects again.

```java
// How serialization works - there and back
UndoManager manager = new UndoManager(4, stack);
UndoManager mgrBack = UndoManager.deserialize(UndoManager.serialize(manager, true));
UndoStack stackBack = mgrBack.getStack();
Point ptBack = (Point)stackBack.getSubject();
```

##  Classes

**Java's Undo Framework** consists of next main classes:

- `UndoCommand` is the base class of all commands stored on an undo stack. It can apply (redo) or undo a single change in the document. `UndoCommand` has default implementation of redo/undo so object of this class can be used as **root** for logically chained commands (see "How FunctionalCommand works" sample)
- `UndoStack` is a list of UndoCommand objects. It contains all the commands executed on the document and can roll the document's state backwards or forwards by undoing or redoing them
- `UndoGroup` is a group of undo stacks. It is useful when an application contains more than one undo stack, typically one for each opened document. UndoGroup provides a single pair of undo/redo slots for all the stacks in the group. It forwards undo and redo requests to the active stack, which is the stack associated with the document that is currently being edited by the user
- `UndoManager` is a class to pass stacks to serialization and back. With such properties like `DATA_VER` and `extras` one can store information for correct deserialization
- `UndoEvents` is an interface for subscriber implementation. It contains all events with default realization so subscriber can realize only required ones

In addition framework contains:

- `FunctionalCommand<V>`: Generic realization for command with ordinary data changing via accessors
- `Getter<V>`: auxiliary interface for standard geter realization
- `Setter<V>`: auxiliary interface for standard seter realization

## Concepts

The following concepts are supported by the framework

- **One subject - one UndoStack**: Keeping subject changes in more than one stack can lead to application crash because of potential clashings and collisions. That's why no way to place 2 or more stacks with one subject in UndoGroup (they compare by address). Stacks that are not in the group it should be controlled by developer.
- **Clean state**: Used to signal when the document enters and leaves a state that has been saved to disk. This is typically used to disable or enable the save actions, and to update the document's title bar
- **Command compression**: Used to compress sequences of commands into a single command. For example: In a text editor, the commands that insert individual characters into the document can be compressed into a single command that inserts whole sections of text. These bigger changes are more convenient for the user to undo and redo
- **Command macros**: A sequence of commands, all of which are undone or redone in one step. These simplify the task of writing an application, since a set of simpler commands can be composed into more complex commands. For example, a command that moves a set of selected objects in a document can be created by combining a set of commands, each of which moves a single object
- **Smart serialization**: Making serialization via UndoManager one can use `DATA_VER` to pass subject and `undo stack` version to the side of deserialization so one can use correct mechanisms to restore data (migration to other version, etc). Moreover, using pairs of property in extras one can send any extra data. UndoManager has ability to compress data using gzip

## HowTo

### Makes command chain without using macrocommands

Use `parent` property

```java
UndoCommand parent = new UndoCommand("Add robot");
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
doc.undoStack().push(parent);
```

#### Makes command chain with macrocommands

```java
doc.undoStack().beginMacro("Add robot");
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().endMacro();
```
