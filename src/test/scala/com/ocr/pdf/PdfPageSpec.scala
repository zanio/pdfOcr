package com.ocr.pdf

import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.awt.image.{BufferedImage, DataBufferByte}
import java.nio.file.Paths
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PdfPageSpec extends AnyFlatSpec with should.Matchers {
  /** Loads a PDF document from the "trivial-pdfs" folder. */
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

  /** Creates a "Hello, world!" document. */
  private def helloWorld: (PdfDocument,PdfPage) = {
    val pdDocument = new PDDocument()
    val pdfDocument = new PdfDocument(Paths.get(""), pdDocument)
    val pdPage = new PDPage()
    pdPage.setMediaBox(new PDRectangle(144, 144))
    pdDocument.addPage(pdPage)
    val stream = new PDPageContentStream(pdDocument, pdPage)

    stream.beginText
    stream.setFont(PDType1Font.HELVETICA, 12)
    stream.showText("Hello, world!")
    stream.endText

    stream.setStrokingColor(255, 0, 0)
    stream.moveTo(10, 80)
    stream.lineTo(90, 80)
    stream.stroke

    stream.close
    (pdfDocument, new PdfPage(pdfDocument, pdPage, 0))
  }

  /** Creates a page that has an invalid stream. */
  private def loadInvalidStreamPage: (PdfDocument,PdfPage) = {
    val pdf = load("2nd-page-invalid.pdf").futureValue

    val pageIt = pdf.pages
    pageIt.next.futureValue // skip page 1
    val page = pageIt.next.futureValue

    (pdf, page)
  }

  private def imageToDataBytes(image: BufferedImage): Array[Array[Byte]] = {
    // Assume 8-bit image data
    image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getBankData
  }

  it should "toText"  in {
      val (document, page) = helloWorld
      try {
        page.toText  should  equal("Hello, world!\n")
      } finally {
        document.close
      }

    // TODO find a way to make PDF cause PdfInvalidException only on second page
    // (PDFBox has gotten resilient....)
    //it should " throw PdfInvalidException"  in {
    //  val (document, page) = loadInvalidStreamPage

    //  try {
    //    a [PdfInvalidException]  should  be thrownBy(page.toText)
    //  } finally {
    //    document.close
    //  }
    //}
  }

  it should "toImage -> should return an image at 300dpi"  in {
      val (document, page) = helloWorld
      page.pdPage.setMediaBox(new PDRectangle(72, 144))

      try {
        val image = page.toImage
        image.getWidth should  equal(300)
        image.getHeight should equal(600)
      } finally {
        document.close
      }

    it should "return an 8-bit grayscale image"  in {
      /*
       * [adam] I don't normally write specs that just duplicate the actual
       * code. I added this one because without it, the next test wouldn't
       * make sense.
       */
      val (document, page) = helloWorld

      try {
        val image = page.toImage
        image.getType should  equal(BufferedImage.TYPE_BYTE_GRAY)
      } finally {
        document.close
      }
    }

    it should "return the correct image data"  in {
      /*
       * If this test fails, do a visual compare and possibly change the image
       * in the classpath.
       */
      val (document, page) = helloWorld

      try {
        val image = page.toImage
        //ImageIO.write(image, "png", new File("pdfPage-toImage-should-return-the-correct-image-data.png"))

        val expected = ImageIO.read(getClass.getResource("/expected-images/pdfPage-toImage-should-return-the-correct-image-data.png"))
        imageToDataBytes(image)  should  equal(imageToDataBytes(expected))
      } finally {
        document.close
      }
    }

    it should "max out at 4000px horizontally"  in {
      val (document, page) = helloWorld
      page.pdPage.setMediaBox(new PDRectangle(7200, 144))

      try {
        val image = page.toImage
        image.getWidth  should  equal(4000)
        image.getHeight  should  equal(80) // 4000 * 144 / 7200
      } finally {
        document.close
      }
    }

    it should " max out at 4000px vertically"  in {
      val (document, page) = helloWorld
      page.pdPage.setMediaBox(new PDRectangle(144, 7200))

      try {
        val image = page.toImage
        image.getWidth  should  equal(80) // 4000 * 144 / 7200
        image.getHeight  should  equal(4000)
      } finally {
        document.close
      }
    }

    // TODO find a way to make PDF cause PdfInvalidException only on second page
    // (PDFBox has gotten resilient....)
    //it should " throw a PdfInvalidException"  in {
    //  val (document, page) = loadInvalidStreamPage

    //  try {
    //    a [PdfInvalidException]  should  be thrownBy(page.toImage)
    //  } finally {
    //    document.close
    //  }
    //}
  }

  it should "toImageWithoutText"  in {
    it should " return an image at 300dpi"  in {
      val (document, page) = helloWorld
      page.pdPage.setMediaBox(new PDRectangle(72, 144))

      try {
        val image = page.toImageWithoutText
        image.getWidth  should  equal(300)
        image.getHeight  should  equal(600)
      } finally {
        document.close
      }
    }

    it should " return the correct image data"  in {
      /*
       * If this test fails, do a visual compare and possibly change the image
       * in the classpath.
       */
      val (document, page) = helloWorld

      try {
        val image = page.toImageWithoutText
        //ImageIO.write(image, "png", new File("pdfPage-toImageWithoutText-should-return-the-correct-image-data.png"))

        val expected = ImageIO.read(getClass.getResource("/expected-images/pdfPage-toImageWithoutText-should-return-the-correct-image-data.png"))
        imageToDataBytes(image)  should  equal(imageToDataBytes(expected))
      } finally {
        document.close
      }
    }

    // TODO find a way to make PDF cause PdfInvalidException only on second page
    // (PDFBox has gotten resilient....)
    //it should " throw a PdfInvalidException"  in {
    //  val (document, page) = loadInvalidStreamPage

    //  try {
    //    a [PdfInvalidException]  should  be thrownBy(page.toImageWithoutText)
    //  } finally {
    //    document.close
    //  }
    //}
  }

  it should "toPdf"  in {
    it should " return"  in {
      val pdf = load("2-pages.pdf").futureValue

      val page = pdf.pages.next.futureValue

      val bytes = page.toPdf

      bytes.length  should  equal(772) // Figured out by running the test
      // Presumably, if this test fails you're editing the toPdf method to add
      // more stringent requirements. You should edit or nix this test when you
      // do.

      new String(bytes, "utf-8")  should  include("/BaseFont /Helvetica")

      pdf.close
    }

    it should " copy an invalid stream"  in {
      val (document, page) = loadInvalidStreamPage

      val bytes = page.toPdf
      bytes.length  should  equal(772) // Figured out by running the test

      document.close
    }

    it should " not produce an error when starting with an owner-protected (not viewer-protected) PDF"  in {
      val pdf = load("owner-protected.pdf").futureValue
      val page = pdf.pages.next.futureValue
      page.toPdf.length  should not equal(0) // really, we want no exception
      pdf.close
    }

    // todo figure out whether there's a way to throw a PdfInvalidException
  }

  it should "addHocr"  in {
    val hocr = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
      <head>
        <title>
          </title>
          <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
          <meta name='ocr-system' content='tesseract 3.04.00' />
          <meta name='ocr-capabilities' content='ocr_page ocr_carea ocr_par ocr_line ocrx_word'/>
        </head>
        <body>
          <div class='ocr_page' id='page_1' title='image "page.png"; bbox 0 0 200 100; ppageno 0'>
            <div class='ocr_carea' id='block_1_4' title="bbox 0 0 200 100">
              <p class='ocr_par' dir='ltr' id='par_1_4' title="bbox 0 0 200 100">
                <span class='ocr_line' id='line_1_24' title="$title"><span class='ocrx_word' id='word_1_326' title='bbox 0 0 200 100' lang='eng' dir='ltr'>OCR!</span>
                </span>
              </p>
            </div>
          </div>
        </body>
      </html>
    """.getBytes("utf-8")

    it should " add text to the page, org.scalatest.tagobjects.Slow"  in {
      val (document, page) = helloWorld

      try {
        page.addHocr(hocr)

        // Internal nonsense: PDDocument needs to subset() its PDFonts before
        // the text extractor can grok them
        document.pdDocument.save(new java.io.ByteArrayOutputStream)

        page.toText  should  equal("Hello, world!\nOCR!\n")
      } finally {
        document.close
      }
    }

    it should " make isFromOcr return true"  in {
      val (document, page) = helloWorld

      try {
        page.addHocr(hocr)
        page.isFromOcr  should  equal(true)
      } finally {
        document.close
      }
    }

    it should " keep isFromOcr false on other pages"  in {
      val (document, page) = helloWorld

      try {
        val pdPage2 = new PDPage()
        document.pdDocument.addPage(pdPage2)
        val page2 = new PdfPage(document, pdPage2, 1)

        page.addHocr(hocr)
        page2.isFromOcr  should  equal(false)
      } finally {
        document.close
      }
    }
  }
}
