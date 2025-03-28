# -*- coding:utf-8 -*-
# --------------------------------------------------------
# Copyright (C), 2016-2020, omosoft, All rights reserved
# --------------------------------------------------------
# @Name:        structs.py
# @Author:      lizhe
# @Created:     2025/3/22 - 15:17
# --------------------------------------------------------
from dataclasses import dataclass
from enum import unique, Enum


@unique
class ModelType(Enum):
    DEEPSEEK = "deepseek"
    DEEPL = "deepl"

    @staticmethod
    def from_name(type_: str):
        for key, item in ModelType.__members__.items():
            if type_.strip().upper() == item.value.upper():
                return item
        raise ValueError(f"{type_} can not be found in {ModelType.__name__}")


@unique
class LanguageType(Enum):
    CHINESE = "简体中文", "ZH"
    SPANISH = "西班牙语", "ES"
    ENGLISH = "英文", "EN"

    @staticmethod
    def from_name(value: str):
        for key, item in LanguageType.__members__.items():
            if value.strip().upper() == item.value[0].upper():
                return item
        raise ValueError(f"{value} can not be found in {LanguageType.__name__}")
