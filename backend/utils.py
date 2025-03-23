# -*- coding:utf-8 -*-
# --------------------------------------------------------
# Copyright (C), 2016-2020, omosoft, All rights reserved
# --------------------------------------------------------
# @Name:        utils.py
# @Author:      lizhe
# @Created:     2025/3/16 - 16:35
# --------------------------------------------------------

import os

from docx import Document


def read_doc_content(file_path: str) -> str:
    """
    读取 .docx 文件的内容。

    参数:
    file_path (str): 文件的绝对路径。

    返回:
    List[str]: 文件的所有内容。
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"文件 {file_path} 不存在。")

    if file_path.endswith('.docx'):
        doc = Document(file_path)
        content = [para.text for para in doc.paragraphs]
    else:
        raise ValueError("仅支持 .docx 文件格式。")

    return "\n".join(content)


def write_doc_content(contents: str, file_name: str):
    """
    将内容写入到 .docx 文件中。

    参数:
    contents (List[str]): 要写入的内容列表。
    file_name (str): 输出文件的名称。
    """
    if not file_name.endswith('.docx'):
        file_name += '.docx'

    doc = Document()
    doc.add_paragraph(contents)

    doc.save(file_name)
