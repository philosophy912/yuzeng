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

from translate import deepseek_evaluate
from utils import write_doc_content, check_file, ERROR_RESPONSE_CODE, check_file_available, \
    read_content, translate_content

app = Flask(__name__)
# 初始化CORS，允许所有域名跨域访问
CORS(app)

@app.route("/evaluate", methods=['POST'])
def evaluate():
    logger.info("enter evaluate interface")
    logger.info(f"{request.form = }")
    result1 = check_file("file1", request.files)
    result2 = check_file("file2", request.files)
    if result1:
        return jsonify(result1), ERROR_RESPONSE_CODE
    if result2:
        return jsonify(result1), ERROR_RESPONSE_CODE
    file1 = request.files['file1']
    file2 = request.files['file2']
    name1, ext1 = os.path.splitext(file1.filename)
    name2, ext2 = os.path.splitext(file2.filename)
    check_result1 = check_file_available(file1, ext1)
    check_result2 = check_file_available(file2, ext2)
    if check_result1:
        return jsonify(check_result1), ERROR_RESPONSE_CODE
    if check_result2:
        return jsonify(check_result2), ERROR_RESPONSE_CODE
    template_file1 = os.path.join(os.getcwd(), f"{name1}_temp{ext1}")
    template_file2 = os.path.join(os.getcwd(), f"{name2}_temp{ext2}")
    file1.save(template_file1)
    file2.save(template_file2)
    content1 = read_content(template_file1, ext1)
    content2 = read_content(template_file2, ext2)
    evaluate_result = deepseek_evaluate(content1, content2)
    name = f"{name1}{ext1}{name2}{ext2}"
    md5_hash = hashlib.md5(name.encode()).hexdigest()
    output_path = os.path.join(os.getcwd(), f"{md5_hash}.docx")
    write_doc_content(evaluate_result, output_path)
    logger.info(f'file evaluate success')
    link = url_for('uploaded_file', filename=output_path, _external=True)
    return jsonify({"message": "评估成功", "status": 20000, "link": link}), 200

@app.route('/translate', methods=['POST'])
def translate():
    logger.info("enter translate interface")
    logger.info(f"{request.form = }")
    # 获取翻译类型和语言类型参数
    model = request.form.get('model')
    language = request.form.get('language')
    logger.info(f"{model = } {language = }")
    result = check_file("file", request.files)
    if result:
        return jsonify(result), ERROR_RESPONSE_CODE
    file = request.files['file']
    name, ext = os.path.splitext(file.filename)
    check_result = check_file_available(file, ext)
    if check_result:
        return jsonify(check_result), ERROR_RESPONSE_CODE
    template_file1 = os.path.join(os.getcwd(), f"{name}_temp{ext}")
    file.save(template_file1)
    content = read_content(template_file1, ext)
    translate_result = translate_content(content, model, language)
    md5_hash = hashlib.md5(name.encode()).hexdigest()
    output_path = os.path.join(os.getcwd(), f"{name}_{md5_hash}.docx")
    write_doc_content(translate_result, output_path)
    logger.info(f'file evaluate success')
    # 返回的下载链接
    link = url_for('uploaded_file', filename=output_path, _external=True)
    return jsonify({"message": "翻译成功", "status": 20000, "link": link}), 200


# 添加新的路由来处理文件下载请求
@app.route('/uploads/<filename>', methods=['GET'])
def uploaded_file(filename):
    return send_from_directory(os.getcwd(), filename, as_attachment=True)


# 添加运行Flask应用的代码
if __name__ == '__main__':
    app.run(debug=True, port=8080, host="0.0.0.0")
