# -*- coding: utf-8 -*-
from docx import Document

doc = Document(r'F:\26毕设单\基于Java的志愿服务活动管理系统的设计与实现\基于Java的志愿服务活动管理系统的设计与实现-初稿.docx')

with open(r'F:\26毕设2\协作编辑系统\docx_content.txt', 'w', encoding='utf-8') as f:
    for i, p in enumerate(doc.paragraphs):
        if p.text.strip():
            style_name = p.style.name if p.style else 'None'
            f.write(f'[{i}] style={style_name} | {p.text}\n')
