[![Telegram](art/badges/badge_tg.svg)](https://t.me/coolperseus)
[![English](art/badges/badge_en.svg)](README.md)
[![Задонатить](art/badges/badge_dontate_ru.svg)](https://dalink.to/dertefter) 


<p align="center">
  <img src="art/banner.png" alt="Баннер">
</p>

# CoolPerseus

Экспериментальный Magisk-модуль для **Xiaomi Mi Mix 3 (Perseus)** с **Lineage OS**.

> [!NOTE]
> Протестировано на официальной LineageOS 22.2. Работа на других прошивках не проверена и не заявлена.


## Возможности

- Приложение для управления настройками слайдера.
- Добавление собственных звуков при открытии/закрытии слайдера
- Настраиваемая анимация при открытии и закрытии слайдера
- Действия при открытии/закрытии сайдера
- Переназначение действий AI-кнопки.

## Установка

Скачайте последнюю версию `CoolPerseus_Magisk.zip` из [Releases](../../releases) и установите её с помощью приложения Magisk.

## Как сделать свой пак звуков

Звуковой пак - это архив zip с набором звуков и превью-картинкой. [Тут](https://t.me/coolperseus) можно найти наборы звуков, созданные пользователями.

##### Что должно быть в архиве:

- Звук открытия слайдера: в названии файла должен быть `_open`
- Звук закрытия слайдера: в названии файла должен быть `_close`
- Картинка-превью: в названии файла должен быть `image`

> [!NOTE]
> Вы можете добавить несколько звуков открытия и закрытия слайдера!
> В этом случае будет проигрываться случайный звук из добавленных.

##### Пример структуры пака:

```
soundpack.zip
├── image.png
├── sound_open_1.ogg
├── sound_open_2.ogg
├── sound_open_3.ogg
├── sound_close_1.ogg
└── sound_close_2.ogg
```


## Сборка из исходников

Если вы хотите самостоятельно собрать модуль, воспользуйтесь прилагаемым скриптом PowerShell.

1. Клонируйте репозиторий:

   ```bash
   git clone https://github.com/dertefter/CoolPerseus.git
   cd CoolPerseus
   ```

2. Убедитесь, что у вас установлен Android SDK и настроена переменная окружения `JAVA_HOME`.

3. Запустите скрипт сборки:
   ```powershell
   ./build_module.ps1
   ```
4. Готовый модуль Magisk будет создан по пути:
   `build/magisk/CoolPerseus_Magisk.zip`.

## 💎 Поддержать меня

[https://dalink.to/dertefter](https://dalink.to/dertefter)