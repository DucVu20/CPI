package sislab.cpi

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

case class CPIParams(
                      address: BigInt        = 0x10020000,
                      useAXI4: Boolean       = false,
                      imgWidth: Int          = 352,
                      imgHeight: Int         = 288,
                      bytePerPixel: Int      = 2,
                      bufferDepth: Int       = 352*288,
                      sccbFreqkHz: Int       = 50,
                      systemFreqMHz: Int     = 50,
                      maxPrescaler: Int      = 32
                    )
object CPIMMIO{
  val interfaceStatus          = 0x00
  val camCapture               = 0x04
  val camMode                  = 0x08
  val imageFormat              = 0x0C
  val returnImgWidth           = 0x10
  val returnImgHeight          = 0x14
  val pixel                    = 0x18
  val pixelAddr                = 0x1C
  val prescaler                = 0x20
}

class CPIIO(val p: CPIParams) extends Bundle{

  val clock     = Input(Clock())
  val reset     = Input(Bool())
  val XCLK      = Output(Clock())
  val prescaler = Input(UInt(log2Ceil(p.maxPrescaler).W))

  val pclk         = Input(Bool())
  val href         = Input(Bool())
  val vsync        = Input(Bool())
  val pixelIn      = Input(UInt(8.W))

  val grayImage    = Input(Bool())
  val capture      = Input(Bool())
  val readFrame    = Input(Bool()) // ready

  val pixelOut     = Output(UInt((8*p.bytePerPixel).W))
  val pixelAddr    = Output(UInt(log2Ceil(p.bufferDepth).W))
  val frameWidth   = Output(UInt(log2Ceil(p.imgWidth).W))
  val frameHeight  = Output(UInt(log2Ceil(p.imgHeight).W))
  val capturing    = Output(Bool())
  val frameFull    = Output(Bool()) // valid

  val config         = Input(Bool())
  val sccbReady      = Output(Bool())
  val SIOC           = Output(Bool())
  val SIOD           = Output(Bool())
  val configData     = Input(UInt(8.W))
  val controlAddress = Input(UInt(8.W))
}

trait CPIPortIO extends Bundle{
  val SIOC    = Output(Bool())
  val SIOD    = Output(Bool())
  val pclk    = Input(Bool())
  val href    = Input(Bool())
  val vsync   = Input(Bool())
  val pixelIn = Input(UInt(8.W))
  val XCLK    = Output(Clock())
}
class CPI2IO extends Bundle{
  val SIOC    = Output(Bool())
  val SIOD    = Output(Bool())
  val pclk    = Input(Bool())
  val href    = Input(Bool())
  val vsync   = Input(Bool())
  val pixelIn = Input(UInt(8.W))
  val XCLK    = Output(Bool())
}

//class CPIInterrupts extends Bundle{
//  val newFrame = Bool()
//}

trait HasCPIIO extends BaseModule{
  val io = IO(new CPIIO(CPIParams.apply()))
}

class CPI(p: CPIParams) extends Module with HasCPIIO {

  val captureModule = Module(new CaptureModule(p.imgWidth, p.imgHeight,
    p.bytePerPixel, p.bufferDepth))
  val SCCBInterface = Module(new SCCBInterface(p.systemFreqMHz, p.sccbFreqkHz))
  val XCLKGenerator = Module(new ClockDivider(p.maxPrescaler))

  captureModule.io.pclk        := io.pclk
  captureModule.io.href        := io.href
  captureModule.io.vsync       := io.vsync
  captureModule.io.pixelIn     := io.pixelIn
  captureModule.io.capture     := io.capture
  captureModule.io.readFrame   := io.readFrame  // to read a frame, read frame signal must be high
  captureModule.io.grayImage   := io.grayImage

  io.pixelOut    := captureModule.io.pixelOut
  io.pixelAddr   := captureModule.io.pixelAddr
  io.frameHeight := captureModule.io.frameHeight
  io.frameWidth  := captureModule.io.frameWidth
  io.capturing   := captureModule.io.capturing
  io.frameFull   := captureModule.io.frameFull

  XCLKGenerator.io.clockIn  := clock
  io.XCLK                    := XCLKGenerator.io.dividedClock
  XCLKGenerator.io.prescaler := io.prescaler
  XCLKGenerator.io.reset     := io.reset

  io.SIOC                    := SCCBInterface.io.SIOC
  io.SIOD                    := SCCBInterface.io.SIOD
  io.sccbReady               := SCCBInterface.io.sccbReady
  SCCBInterface.io.config         := io.config
  SCCBInterface.io.configData     := io.configData
  SCCBInterface.io.controlAddress := io.controlAddress
}
trait CPIModule extends HasRegMap{
  val io: CPIPortIO
  implicit val p: Parameters
  def params: CPIParams
  val clock: Clock
  val reset: Reset

  val pixel = Wire(new DecoupledIO(UInt((params.bytePerPixel*8).W)))
  val CPI   = Module(new CPI(params))

  val status             = Wire(UInt(3.W))
  val captureFrame       = WireInit(false.B)
  val cameraMode         = Wire(DecoupledIO(UInt(16.W)))
  val pixelAddr          = Wire(UInt(CPI.io.pixelAddr.getWidth.W))
  val prescaler          = Reg(UInt(log2Ceil(params.maxPrescaler).W))
  val imageFormat        = Reg(UInt(1.W))

  CPI.io.clock := clock
  CPI.io.reset := reset.asBool

  CPI.io.pclk    := io.pclk
  CPI.io.href    := io.href
  CPI.io.vsync   := io.vsync
  CPI.io.pixelIn := io.pixelIn

  io.SIOC := CPI.io.SIOC
  io.SIOD := CPI.io.SIOD

  io.XCLK          := CPI.io.XCLK
  CPI.io.prescaler := prescaler

  CPI.io.controlAddress := cameraMode.bits(15,8)
  CPI.io.configData     := cameraMode.bits(7,0)
  CPI.io.config         := cameraMode.valid
  cameraMode.ready      := CPI.io.sccbReady

  CPI.io.capture        := captureFrame

  CPI.io.grayImage   := imageFormat
  pixel.bits         := CPI.io.pixelOut
  pixel.valid        := CPI.io.frameFull
  CPI.io.readFrame   := pixel.ready
  pixelAddr          := CPI.io.pixelAddr


  status := Cat(CPI.io.sccbReady, CPI.io.frameFull, CPI.io.capturing)


  regmap(
    CPIMMIO.interfaceStatus -> Seq(
      RegField.r(3,status)),
    CPIMMIO.camCapture -> Seq(
      RegField.w(1,captureFrame)),
    CPIMMIO.camMode -> Seq(
      RegField.w(16,cameraMode)),
    CPIMMIO.imageFormat -> Seq(
      RegField.w(1,imageFormat)
    ),
    CPIMMIO.returnImgWidth -> Seq(
      RegField.r(CPI.io.frameWidth.getWidth,CPI.io.frameWidth)
    ),
    CPIMMIO.returnImgHeight -> Seq(
      RegField.r(CPI.io.frameHeight.getWidth, CPI.io.frameHeight)
    ),
    CPIMMIO.pixel -> Seq(
      RegField.r(16,pixel)),
    CPIMMIO.pixelAddr -> Seq(
      RegField.r(CPI.io.pixelAddr.getWidth, pixelAddr)
    ),
    CPIMMIO.prescaler -> Seq(
      RegField.w(log2Ceil(params.maxPrescaler),prescaler)
    )
  )
}

class CPITL(params: CPIParams,beatBytes: Int)(implicit  p:Parameters)
  extends TLRegisterRouter(
    params.address,"OV7670", Seq("ucbbar,OV7670"),
    beatBytes=beatBytes)(
    new TLRegBundle(params, _) with CPIPortIO)(
    new TLRegModule(params, _, _) with CPIModule)

class CPIAXI4(params: CPIParams, beatBytes: Int)(implicit p: Parameters)

  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
    new AXI4RegBundle(params, _) with CPIPortIO)(
    new AXI4RegModule(params, _, _) with CPIModule)

case object CPIKey extends Field[Option[CPIParams]](None)

trait CanHavePeripheryCPI {this : BaseSubsystem =>
  private val portName= "OV7670"

  val cpi=p(CPIKey) match{
    case Some(params) => {
      if (params.useAXI4) {
        val cpi = LazyModule(new CPIAXI4(params, pbus.beatBytes)(p))
        pbus.toSlave(Some(portName)) {
          cpi.node :=
            AXI4Buffer () :=
            TLToAXI4 () :=
            // toVariableWidthSlave doesn't use holdFirstDeny, which TLToAXI4() needsx
            TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
        }
        Some(cpi)
      } else {
        val cpi = LazyModule(new CPITL(params, pbus.beatBytes)(p))
        pbus.toVariableWidthSlave(Some(portName)) { cpi.node }
        Some(cpi)
      }
    }
    case None => None
  }
}

trait CanHavePeripheryCPIModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryCPI
  val port = outer.cpi match {
    case  Some(cpi) => {
      val cpiPort=IO(new CPI2IO)

      cpiPort.SIOC          := cpi.module.io.SIOC
      cpiPort.SIOD          := cpi.module.io.SIOD
      cpi.module.io.vsync   := cpiPort.vsync
      cpi.module.io.href    := cpiPort.href
      cpi.module.io.pclk    := cpiPort.pclk
      cpi.module.io.pixelIn := cpiPort.pixelIn
      cpiPort.XCLK          := cpi.module.io.XCLK.asBool

      Some(cpiPort)
     // dontTouch(cpiPort)
    }
    case None => None
  }
}

class WithOV7670(useAXI4: Boolean) extends Config((site,here, up) => {
  case CPIKey => Some(CPIParams(useAXI4=useAXI4))
})
