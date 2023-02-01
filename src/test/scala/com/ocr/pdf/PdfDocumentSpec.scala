package com.ocr.pdf


import com.ocr.exceptions.{PdfEncryptedException, PdfInvalidException}
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.io.IOException
import java.nio.file.{Files, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PdfDocumentSpec  extends AnyFlatSpec with should.Matchers {
  private def load(resourceName: String): Future[PdfDocument] = {
    val pathString: String = try {
      getClass.getResource(s"/trivial-pdfs/$resourceName").toString.replace("file:", "")
    } catch {
      case ex: NullPointerException => {
        throw new Exception(s"Missing test file /trivial-pdfs/$resourceName")
      }
    }
    val path = Paths.get(pathString)
    PdfDocument.load(path)
  }


    it should "loads a valid PDF" in {
      val pdfDocument = load("empty-page.pdf").futureValue
      pdfDocument.path.getFileName.toString should equal("empty-page.pdf")
      pdfDocument.close
    }

    it should "throws PdfEncryptedException" in {
        a[PdfEncryptedException] should be thrownBy {
          load("empty-page-encrypted.pdf").futureValue
        }
    }

    it should "throws PdfInvalidException when the file is not a PDF" in {
      val ex = load("not-a-pdf.pdf").failed.futureValue
      a[PdfInvalidException] should be thrownBy ex
    }

    it should "throws IOException when the file does not exist" in {
      a[IOException] should be thrownBy PdfDocument.load(Paths.get("/this/path/is/very/unlikely/to/exist.pdf")).futureValue
    }

    it should "removes the owner password (so long as there is no user password)" in {
      val pdfDocument = load("owner-protected.pdf").futureValue
      val outPath = Files.createTempFile("pdfocr-test-pdfdocument-", ".pdf")
      pdfDocument.write(outPath).futureValue should  equal(())
      Files.delete(outPath)
    }


    it should "returns the number of pages" in {
      val pdf = load("empty-page.pdf").futureValue
      pdf.nPages should  equal(1)
      pdf.close
    }


    it should "iterates over each page" in {
      val pdf = load("2-pages.pdf").futureValue
      val it = pdf.pages
      try {
        it.hasNext should  equal(true)
        it.next.futureValue.toText should  equal("Page 1\n")
        it.hasNext should equal(true)
        it.next.futureValue.toText should equal("Page 2\n")
        it.hasNext should equal(false)
      } finally {
        pdf.close
      }
    }

    it should "return a page even if it contains an invalid stream" in {
      val pdf = load("2nd-page-invalid.pdf").futureValue
      try {
        val it = pdf.pages
        it.next.futureValue
        it.next.futureValue
        it.hasNext should  equal(false)
      } finally {
        pdf.close
      }
    }

}
