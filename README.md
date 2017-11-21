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

