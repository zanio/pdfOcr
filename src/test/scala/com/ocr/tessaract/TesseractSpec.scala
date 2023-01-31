package com.ocr.tessaract

import com.ocr.exceptions.{TesseractFailedException, TesseractLanguageMissingException, TesseractMissingException}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.awt.image.BufferedImage
import java.util.Locale
import scala.concurrent.ExecutionContext.Implicits.global

class TesseractSpec extends AnyFlatSpec with should.Matchers {
  val tesseractPath: String = getClass.getResource("/fake-tesseract").toString.replace("file:", "")
  val tesseractOptions: TesseractOptions = TesseractOptions(tesseractPath)
  val tesseract: Tesseract = new Tesseract(tesseractOptions)
  val image: BufferedImage = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY)

  new java.io.File(tesseractPath).setExecutable(true) // because sbt's copying nixes the executable bit

  it should "shells to Tesseract and collects stderr" in {
    val result = tesseract.ocr(image, Seq(new Locale("en"))).futureValue
    new String(result.standardError, "utf-8") should equal("-l eng --psm 1 --oem 1 - - hocr\n")
  }

  it should "concatenates languages using +" in {
    val result = tesseract.ocr(image, Seq(new Locale("en"), new Locale("fr"))).futureValue
    new String(result.standardError, "utf-8") should equal("-l eng+fra --psm 1 --oem 1 - - hocr\n")
  }

  it should "sends Tesseract the image as BMP" in {
    val result = tesseract.ocr(image, Seq(new Locale("en"))).futureValue
    // We won't test every pixel, but we can test it's a BMP. (Our test script
    // just outputs its input.)
    val bytes = result.standardOutput
    bytes(0) should equal(0x42)
    bytes(1) should equal(0x4d)

    // BMP: file size is in the header, as 4-byte integer
    bytes.length should equal((bytes(5) & 0xff) << 24 | (bytes(4) & 0xff) << 16 | (bytes(3) & 0xff) << 8 | bytes(2) & 0xff)
  }

  it should "throws TesseractMissingException" in {
    val tesseract2 = new Tesseract(TesseractOptions("/invalid-tesseract-path"))
    tesseract2.ocr(image, Seq(new Locale("en"))).failed.futureValue shouldBe a[TesseractMissingException]
  }

  it should "throws TesseractLanguageMissingException" in {
    val exception = tesseract.ocr(image, Seq(new Locale("zxx"))).failed.futureValue
    exception shouldBe a[TesseractLanguageMissingException]
    exception.asInstanceOf[TesseractLanguageMissingException].language should equal("zxx")
  }

  it should "throws TesseractLanguageMissingException when the missing language is `osd`" in {
    val exception = tesseract.ocr(image, Seq(new Locale("osd"))).failed.futureValue
    exception shouldBe a[TesseractLanguageMissingException]
    exception.asInstanceOf[TesseractLanguageMissingException].language should equal("osd")
  }

  it should "throws TesseractUnknownException" in {
    val exception = tesseract.ocr(image, Seq(new Locale("und"))).failed.futureValue
    exception shouldBe a[TesseractFailedException]
    exception.asInstanceOf[TesseractFailedException].retval should equal(1)
    exception.asInstanceOf[TesseractFailedException].stderr should equal("An error message\n")
  }
}
