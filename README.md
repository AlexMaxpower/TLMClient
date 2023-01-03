# Приложение TLMClient

TLMClient — приложение для получения по UDP протоколу телеметрической информации и отображения ее в наглядном виде.

## Основные возможности

* Выделение данных телеметрии из общего потока информации
* Проверка целостности данных по контрольной сумме
* Вывод полученных данных в наглядном графическом виде (таблица)
* Подсветка ошибок

## Структура телеметрического пакета

| Название            | Тип данных | Примечание                             |
|---------------------|------------|----------------------------------------|
| Синхромаркер        | uint32     | Всегда должен быть равен 0x12345678    |
| Счетчик пакетов     | uint32     | Счетчик пакетов, инкрементируется на 1 |
| Время               | double     | Количество секунд от 1970 г.           |
| Полезная информация | double     | Случайное число                        |
| Контрольная сумма   | uint16     | CRC16_CCITT-FALSE всего пакета         |

## Требования 
* Все данные представлены в LITTLE_ENDIAN
* Сервер отправляет пакеты на сокет 15000
* Задание должно быть выполнено на языке Java с использованием библиотек Swing или JavaFx

## Окно программы
![Окно программы](tlmclient-color.png)
