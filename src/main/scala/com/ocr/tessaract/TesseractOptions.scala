package com.ocr.tessaract

case class TesseractOptions(
  tesseractPath: String = "tesseract"
)

object TesseractOptions {
  val Default = TesseractOptions()
}
