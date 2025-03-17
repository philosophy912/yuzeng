# -*- coding:utf-8 -*-
# --------------------------------------------------------
# Copyright (C), 2016-2020, omosoft, All rights reserved
# --------------------------------------------------------
# @Name:        main.py.py
# @Author:      lizhe
# @Created:     2025/3/12 - 22:29
# --------------------------------------------------------
import hashlib  # 添加hashlib库用于计算MD5值
import os

from flask import Flask, request, jsonify, url_for, send_from_directory
# 导入flask_cors库
from flask_cors import CORS
from loguru import logger

from translate import deepseek_translate, deepseek_evaluate
from utils import read_doc_content, write_doc_content

app = Flask(__name__)
# 初始化CORS，允许所有域名跨域访问
CORS(app)


@app.route('/dashboard/items', methods=['GET'])
def dashboard_item():
    return "hello world"


@app.route('/upload', methods=['POST'])
def upload_file():
    logger.info("enter upload interface")
    logger.info(f"{request.form = }")
    # 获取翻译类型和语言类型参数
    model_type = request.form.get('modelType')
    translation_type = request.form.get('translationType')
    language_type = request.form.get('languageType')
    logger.info(f"{model_type = } {translation_type = } {language_type = }")

    # 检查请求中是否包含文件部分
    if 'file' not in request.files:
        return jsonify({"message": "没有文件部分", "status": 40000, "link": ""}), 400
    file = request.files['file']
    logger.info(f"{file = }")
    # 检查文件名是否为空
    if file.filename == '':
        return jsonify({"message": "没有选择文件", "status": 40000, "link": ""}), 400

    # 获取文件扩展名
    name, ext = os.path.splitext(file.filename)
    logger.info(f"filename = {file.filename}")
    # 计算文件名的MD5值
    md5_hash = hashlib.md5(name.encode()).hexdigest()
    allowed_extensions = {'.doc', '.docx', '.txt', '.md'}
    if ext.lower() not in allowed_extensions:
        return jsonify({"message": "不支持的文件类型", "status": 40000, "link": ""}), 400

    # 检查文件大小
    max_size = 100 * 1024 * 1024  # 100MB
    if file.content_length > max_size:
        return jsonify({"message": "文件大小超过限制", "status": 40000, "link": ""}), 400

    template_file = os.path.join(os.getcwd(), f"{name}_temp{ext}")

    file_path = os.path.join(os.getcwd(), template_file)
    file.save(template_file)

    # 修改为保存到当前文件夹下
    new_filename = f"{name}_{md5_hash}{ext}"
    output_path = os.path.join(os.getcwd(), new_filename)
    contents = read_doc_content(file_path)
    if translation_type == "文本翻译":
        result_content = deepseek_translate(contents, language_type)
        logger.debug(f"{result_content = }")
        write_doc_content(result_content, output_path)

    logger.info(f'file save success {new_filename}')

    # 返回的下载链接
    link = url_for('uploaded_file', filename=new_filename, _external=True)
    return jsonify({"message": "文件上传成功", "status": 20000, "link": link}), 200


# 添加新的路由来处理文件下载请求
@app.route('/uploads/<filename>', methods=['GET'])
def uploaded_file(filename):
    return send_from_directory(os.getcwd(), filename, as_attachment=True)


# 添加运行Flask应用的代码
if __name__ == '__main__':
    app.run(debug=True, port=8080, host="0.0.0.0")
