# -*- coding:utf-8 -*-
# --------------------------------------------------------
# Copyright (C), 2016-2020, omosoft, All rights reserved
# --------------------------------------------------------
# @Name:        translate.py
# @Author:      lizhe
# @Created:     2025/3/17 - 09:06
# --------------------------------------------------------
import os
from typing import Tuple
import deepl

from loguru import logger  # 导入 logger 模块
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv('DEEPSEEK_KEY'),
    base_url="https://api.deepseek.com"
)


def deepseek_translate(content: str, target_lang: str) -> str:
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {
                "role": "system",
                "content": f"你是一个语言翻译专家，将用户输入的内容准确翻译成{target_lang}"
                           "用户可以向助手发送需要翻译的内容，助手会回答相应的翻译结果，并确保符合中文语言习惯，你可以调整语气和风格，"
                           "并考虑到某些词语的文化内涵和地区差异。同时作为翻译家，需将原文翻译成具有信达雅标准的译文。\"信\" 即忠实于原文的内容与意图；"
                           "\"达\" 意味着译文应通顺易懂，表达清晰；\"雅\" 则追求译文的文化审美和语言的优美。目标是创作出既忠于原作精神，又符合目标语言文化和读者审美的翻译。"
            },
            {
                "role": "user",
                "content": f"将以下内容准确翻译成{target_lang}，保留格式标记：\n{content}"
            }
        ],
        temperature=1.3,  # 翻译参数
        max_tokens=3000
    )
    translated_content = response.choices[0].message.content
    logger.debug("DeepSeek 翻译成功")
    safe_content = translated_content.encode('utf-8', errors='xmlcharrefreplace').decode('utf-8')
    return safe_content


def deepseek_evaluate(source_text: str, translated_text: str) -> str:
    evaluation_prompt = f"""
    你是一名法律翻译专家。请检查以下人工翻译合同的质量，并指出以下方面的问题：
    - 术语一致性
    - 语法错误
    - 法律表达是否准确
    - 结构是否符合合同格式
    原文（西班牙语）：
    {source_text}
    翻译（中文）：
    {translated_text}
    请列出发现的问题，并提供修改建议：
    """
    response = client.chat.completions.create(
        model="deepseek-reasoner",  # 需要调用的模型
        messages=[
            {"role": "system", "content": "你是一名资深法律翻译审核专家，需要严格检查翻译质量"},
            {"role": "user", "content": evaluation_prompt}
        ],
        temperature=1.0,
        max_tokens=2000
    )
    evaluation_result = response.choices[0].message.content
    return evaluation_result


def deepl_translate(content: str, target_lang: str) -> str:
    api_key = os.getenv('DEEPL_KEY')
    translator = deepl.Translator(api_key)
    result = translator.translate_text(
        content,
        target_lang=target_lang.upper(),
        formality='prefer_more'
    )
    logger.debug("DeepL 翻译成功")
    return result.text
