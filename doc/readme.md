# Java Undo Framework

## Введение

**Java Undo Framework** это реализация паттерна **Команда** для создания в приложении цепочек Undo/Redo.
`Java Undo Framework is an implementation of the Command pattern, for implementing undo/redo functionality in applications.`

Паттерн **Команда** базируется на предположении, что все изменения субъектов приложения совершаются посредством создания объектов соответствующих команд. Объекты команд сохраняют состояние приложения, совершают необходимые изменения и последовательно сохраняются в стеке команд. Соответственно, каждая команда знает, как вернуть приложение в предыдущее состояние. До тех пор, пока модель "изменений через команды" не нарушается, всегда имеется возможность откатиться на любой момент, просто последовательно выполняя Undo, команда за командой в стеке, и вернуться обратно, выполняя Redo в направлении "вперед".
`The Command pattern is based on the idea that all editing in an application is done by creating instances of command objects. Command objects apply changes to the document and are stored on a command stack. Furthermore, each command knows how to undo its changes to bring the document back to its previous state. As long as the application only uses command objects to change the state of the document, it is possible to undo a sequence of commands by traversing the stack downwards and calling undo on each command in turn. It is also possible to redo a sequence of commands by traversing the stack upwards and calling redo on each command.`

**Java Undo Framework** вдохновлен реализацией такого фреймворка на Qt, с небольшими изменениями и дополнениями. Так, добавлена шаблонная реализация UndoCommand, позволяющая создать команду для простого изменения свойства без создания класса (FunctionalCommand<V>). Кроме того, добавлена сериализация стеков (с zip-компрессией и без) в строку Base64 на базе стандартного механизма сериализации объектов Java.
`TODO: добавить`

## Классы

Фреймворк состоит из следующих основных классов и интерфейсов:
`The framework consists of:`

- class **UndoCommand**: базовый класс для команд, хранимых в стеке. Применяет команду redo/undo для атомарного изменения в документе
- `UndoCommand is the base class of all commands stored on an undo stack. It can apply (redo) or undo a single change in the document`
- class **UndoStack**: лист команд. Содержит лист последовательно добавленных команд, выполненных над определенным субъектом документа и может "откатывать" состояние документа до любого момента вперед и назад
- `UndoStack is a list of UndoCommand objects. It contains all the commands executed on the document and can roll the document's state backwards or forwards by undoing or redoing them`
- class **UndoGroup**: группа стеков. Группировка стеков удобна, когда приложение имеет больше, чем один документ, и необходимо для каждого хранить индивидуальное состояние undo/redo. UndoGroup имеет свойство "активный стек", позволяющий бесшовно переключаться между документами и выполнять undo/redo над каждым из них
- `UndoGroup is a group of undo stacks. It is useful when an application contains more than one undo stack, typically one for each opened document. UndoGroup provides a single pair of undo/redo slots for all the stacks in the group. It forwards undo and redo requests to the active stack, which is the stack associated with the document that is currently being edited by the user`
- class **UndoManager**: Полезный класс для (де)сериализации стека. Содержит два статических метода для (де)сериализации. Кроме того, дополнительная информация, которую можно сохранить в менеджере при сериализации, позволяет узнать, как правильно интерпретировать субъекты на стороне десериализации, в частности - версию субъектов (что полезно при миграции данных)
- `UndoManager...`
- interface **UndoEvents**: набор событий для подписчиков, нуждающихся в информации о событиях стека команд. Все методы дефолтные, так что подписчик не обязан реализовывать то, что ему не надо
- `UndoEvents...`

Дополнительные классы и интерфейсы:

- class **FunctionalCommand<V>**: Удобная шаблонная команда, которая позволяет реализовать простое изменение данных без создания дополнительных классов
- `class FunctionalCommand<V>...`
- interface **Getter<V>**: Вспомогательный интерфейс для свойства-геттера команды FunctionalCommand<V>
- `interface Getter<V>...`
- interface **Setter<V>**: Вспомогательный интерфейс для свойства-сеттера команды FunctionalCommand<V>
- `interface Setter<V>...`

## Концепции

Фреймворк поддерживает следующие концепции:
`The following concepts are supported by the framework:`

- Один субъект - один стек: Хранить изменения над одним субъектом в разных стеках не только нелогично, но и опасно с точки зрения устойчивости приложения при выполнении undo/redo, возможны самые разные коллизии. Поэтому в группе невозможно разместить 2 стека с одинаковым субъектом. Хотя это не поможет, если разработчик решит использовать такую потенциально опасную ситуацию для несгруппированных стеков
- `Один субъект - один стек...`
- Чистое состояние: Используется для сигнализации, что документ вошел или покинул момент, когда происходило сохранение на диск. Это можно использовать для отражения в состоянии зависимых визуальных контролов приложения (доступность кнопки "Сохранить", и т.п.)
- `Clean state: Used to signal when the document enters and leaves a state that has been saved to disk. This is typically used to disable or enable the save actions, and to update the document's title bar`
- Компрессия команд: Используется для объединения однотипных последовательностей в единую команду. В текстовом документе можно воспользоваться для объединения печатания одиночных символов в команду, печатающую целое слово. В графическом многократное перемещение объекта в одно перемещение от стартовой до конечной точки
- `Command compression: Used to compress sequences of commands into a single command. For example: In a text editor, the commands that insert individual characters into the document can be compressed into a single command that inserts whole sections of text. These bigger changes are more convenient for the user to undo and redo`
- Макрокоманды: Последовательность команд, выполняемых undo/redo за один раз. Это упрощает создание задач для приложений, когда совокупность взаимосвязанных команд должна быть выполнена одномоментно. К примеру, удаление/восстановление множества выделенных объектов графической сцены можно оформить как макрос "Удаление выделенных объектов".
- `Command macros: A sequence of commands, all of which are undone or redone in one step. These simplify the task of writing an application, since a set of simpler commands can be composed into more complex commands. For example, a command that moves a set of selected objects in a document can be created by combining a set of commands, each of which moves a single object`

### Howto

#### Создать цепочку команд без использования макросов
Создать базовую команду типа UndoCommand, а для всех последующих указать ее, как парент. В стек добавлять только первую команду. Таким образом все последующие команды окажутся не в стеке, а в листе субкоманд первой команды, и автоматически выполнятся при вызове `UndoCommand.undo()`:

```
UndoCommand parent = new UndoCommand("Add robot");
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
new AddShapeCommand(doc, ShapeRectangle, parent);
doc.undoStack().push(parent);
```

#### Создать цепочку команд с макросом
```
doc.undoStack().beginMacro("Add robot");
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().push(new AddShapeCommand(doc, ShapeRectangle, parent));
doc.undoStack().endMacro();
```

