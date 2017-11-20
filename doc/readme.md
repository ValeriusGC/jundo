# Java Undo Framework

## Введение

**Java Undo Framework** это реализация паттерна **Команда** для создания в приложении цепочек Undo/Redo.
`Java Undo Framework is an implementation of the Command pattern, for implementing undo/redo functionality in applications.`

Паттерн **Команда** базируется на предположении, что все изменения объектов приложения совершаются посредством создания объектов соответствующих команд. Объекты команд сохраняют состояние приложения, совершают необходимые изменения и последовательно сохраняются в стеке команд. Соответственно, каждая команда знает, как вернуть приложение в предыдущее состояние. До тех пор, пока модель "изменений через команды" не нарушается, всегда имеется возможность откатиться на любой момент, просто последовательно выполняя Undo, команда за командой в стеке, и вернуться обратно, выполняя Redo в направлении "вперед".
`The Command pattern is based on the idea that all editing in an application is done by creating instances of command objects. Command objects apply changes to the document and are stored on a command stack. Furthermore, each command knows how to undo its changes to bring the document back to its previous state. As long as the application only uses command objects to change the state of the document, it is possible to undo a sequence of commands by traversing the stack downwards and calling undo on each command in turn. It is also possible to redo a sequence of commands by traversing the stack upwards and calling redo on each command.`

**Java Undo Framework** вдохновлен реализацией такого фреймворка на Qt, с небольшими изменениями и дополнениями. Так, добавлена шаблонная реализация UndoCommand, позволяющая создать команду для простого изменения свойства без создания класса (FunctionalCommand<V>). Кроме того, добавлена сериализация стеков (с компрессией и без) в строку Base64 на базе стандартного механизма сериализации объектов Java.
`TODO: добавить`

## Классы

