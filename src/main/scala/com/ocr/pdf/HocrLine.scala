package com.ocr.pdf

import java.awt.Rectangle

case class HocrLine(boundingBox: Rectangle, words: Seq[HocrWord])
