package uk.co.workingedge.gemini.x

import android.util.Base64
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONArray
import org.json.JSONObject
import uk.co.workingedge.gemini.x.lib.BlobHistoryPart

import uk.co.workingedge.gemini.x.lib.GeminiX
import uk.co.workingedge.gemini.x.lib.HistoryItem
import uk.co.workingedge.gemini.x.lib.HistoryPart
import uk.co.workingedge.gemini.x.lib.Image
import uk.co.workingedge.gemini.x.lib.ImageHistoryPart
import uk.co.workingedge.gemini.x.lib.TextHistoryPart

class GeminiXModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  private val GeminiXResponseChunkEvent = "GeminiXResponseChunk"

  /*************************************************************************
   * Plugin Methods
   ************************************************************************/
  @ReactMethod
  fun initModel(params: ReadableMap, promise: Promise) {
    try {
      // Required params
      val modelName = params.getString("modelName")!!
      val apiKey = params.getString("apiKey")!!

      // Optional params
      var temperature: Float? = null
      if (params.hasKey("temperature")) {
        temperature = params.getDouble("temperature").toFloat()
      }

      var topK: Int? = null
      if (params.hasKey("topK")) {
        topK = params.getInt("topK")
      }

      var topP: Float?  = null
      if (params.hasKey("topP")) {
        topP = params.getDouble("topP").toFloat()
      }

      var maxOutputTokens: Int?  = null
      if (params.hasKey("maxOutputTokens")) {
        maxOutputTokens = params.getInt("maxOutputTokens")
      }

      var stopSequences: List<String>? = null
      if (params.hasKey("stopSequences")) {
        val stopSequencesReadableArray = params.getArray("stopSequences")
        val stopSequencesArray = arrayListOf<String>()
        for (i in 0 until stopSequencesReadableArray!!.size()) {
          stopSequencesArray.add(stopSequencesReadableArray.getString(i))
        }
        stopSequences = stopSequencesArray.toList()
      }

      val config = mutableMapOf<String, Any>()
      if (temperature != null) {
        config["temperature"] = temperature
      }
      if (topK != null) {
        config["topK"] = topK
      }
      if (topP != null) {
        config["topP"] = topP
      }
      if (maxOutputTokens != null) {
        config["maxOutputTokens"] = maxOutputTokens
      }
      if (stopSequences != null) {
        config["stopSequences"] = stopSequences
      }

      var safetySettings: Map<String, String>? = null
      if (params.hasKey("safetySettings")) {
        val safetySettingsReadableMap = params.getMap("safetySettings")
        val safetySettingsMap = mutableMapOf<String, String>()
        for (key in safetySettingsReadableMap!!.toHashMap().keys) {
          safetySettingsMap[key] = safetySettingsReadableMap.getString(key)!!
        }
        safetySettings = safetySettingsMap.toMap()
      }

      GeminiX.init(modelName, apiKey, config, safetySettings)
      promise.resolve(null)
    }catch (e: Exception) {
      promise.reject(e)
    }
  }

  @ReactMethod
  fun sendMessage(call:ReadableMap, promise: Promise) {
    try {
      var streamResponse = false
      val images = JSONArray()

      val inputText = call.getString("inputText")!!
      if(call.hasKey("options")){
        val opts = call.getMap("options")
        if(opts!!.hasKey("streamResponse")){
          streamResponse = opts.getBoolean("streamResponse")
        }
        if(opts.hasKey("images")){
          val readableImages = opts.getArray("images")
          for (i in 0 until readableImages!!.size()) {
              images.put(readableImages.getString(i))
          }
        }
      }

      val modelImages:List<Image> = GeminiX.getModelImages(images, this.reactApplicationContext)

      GeminiX.sendMessage(
        { response, isComplete ->
          val result = Arguments.createMap()
          result.putString("response", response)
          if(isComplete){
            promise.resolve(result)
          }else{
            result.putBoolean("isChat", false)
            sendEvent(this.reactApplicationContext, GeminiXResponseChunkEvent, result)
          }
        },
        { error ->
          promise.reject("Calling GeminiX failed", error)
        }, inputText, modelImages, streamResponse)
    } catch (e: Exception) {
      promise.reject("Invoking sendMessage failed", e)
    }
  }

  @ReactMethod
  fun countTokens(call: ReadableMap, promise: Promise) {
    try {
      val images = JSONArray()

      val inputText = call.getString("inputText")!!
      if(call.hasKey("options")){
        val opts = call.getMap("options")
        if(opts!!.hasKey("images")){
          val readableImages = opts.getArray("images")
          for (i in 0 until readableImages!!.size()) {
            images.put(readableImages.getString(i))
          }
        }
      }

      val modelImages:List<Image> = GeminiX.getModelImages(images, this.reactApplicationContext)

      GeminiX.countTokens(
        { response ->
          val result = Arguments.createMap()
          result.putInt("count", response)
          result.putBoolean("isChat", false)
          promise.resolve(result)
        },
        { error ->
            promise.reject("Calling GeminiX failed", error)
        }, inputText, modelImages)
    } catch (e: Exception) {
      promise.reject("Invoking countTokens failed", e)
    }
  }

  @ReactMethod
  fun initChat(call: ReadableMap, promise: Promise) {
    try {
      val history: MutableList<HistoryItem> = mutableListOf()
      if(call.hasKey("chatHistory")){
        val jsonHistory = call.getArray("chatHistory")

        for (i in 0 until jsonHistory!!.size()) {
          val item = jsonHistory.getMap(i)
          val isUser = item.getBoolean("isUser")

          val historyParts: MutableList<HistoryPart> = mutableListOf()

          if(item.hasKey("text")){
            val text = item.getString("text");
            val historyPart: HistoryPart = TextHistoryPart(text!!)
            historyParts.add(historyPart)
          }

          if(item.hasKey("images")){
            val images = item.getArray("images")
            for (j in 0 until images!!.size()) {
              val image = images.getMap(j)
              val uri = image.getString("uri")
              val bitmap = GeminiX.getBitmapFromUri(uri!!, this.reactApplicationContext)
              var historyPart: HistoryPart?
              if(image.hasKey("mimeType")){
                val mimeType = image.getString("mimeType")
                val blob = GeminiX.bitmapToByteArray(bitmap)
                historyPart = BlobHistoryPart(blob, mimeType!!)
              }else{
                historyPart = ImageHistoryPart(bitmap)
              }
              historyParts.add(historyPart)
            }
          }
          val historyItem = HistoryItem(historyParts, isUser)
          history.add(historyItem)
        }
      }

      GeminiX.initChat(
        {
          promise.resolve(null)
        },
        { error ->
          promise.reject("Calling GeminiX failed", error)
        }, history)
    } catch (e: Exception) {
      promise.reject("Invoking initChat failed", e)
    }
  }

  @ReactMethod
  fun sendChatMessage(call: ReadableMap, promise: Promise) {
    try {
      var streamResponse = false
      val images = JSONArray()

      val inputText = call.getString("inputText")!!
      if(call.hasKey("options")){
        val opts = call.getMap("options")
        if(opts!!.hasKey("streamResponse")){
          streamResponse = opts.getBoolean("streamResponse")
        }
        if(opts.hasKey("images")){
          val readableImages = opts.getArray("images")
          for (i in 0 until readableImages!!.size()) {
            images.put(readableImages.getString(i))
          }
        }
      }

      val modelImages:List<Image> = GeminiX.getModelImages(images, this.reactApplicationContext)


      GeminiX.sendChatMessage(
        { response, isComplete ->
          val result = Arguments.createMap()
          result.putString("response", response)
          if(isComplete){
            promise.resolve(result)
          }else{
            result.putBoolean("isChat", true)
            sendEvent(this.reactApplicationContext, GeminiXResponseChunkEvent, result)
          }
        },
        { error ->
          promise.reject("Calling GeminiX failed", error)
        }, inputText, modelImages, streamResponse)
    } catch (e: Exception) {
      promise.reject("Invoking sendChatMessage failed", e)
    }
  }

  @ReactMethod
  fun countChatTokens(call: ReadableMap, promise: Promise) {
    try {
      var inputText:String? = null
      val images = JSONArray()

      if(call.hasKey("options")){
        val opts = call.getMap("options")
        if(opts!!.hasKey("images")){
          val readableImages = opts.getArray("images")
          for (i in 0 until readableImages!!.size()) {
            images.put(readableImages.getString(i))
          }
        }
        if(opts.hasKey("inputText")){
          inputText = opts.getString("inputText")
        }
      }

      val modelImages:List<Image> = GeminiX.getModelImages(images, this.reactApplicationContext)

      GeminiX.countChatTokens(
        { response ->
          val result = Arguments.createMap()
          result.putInt("count", response)
          result.putBoolean("isChat", true)
          promise.resolve(result)
        },
        { error ->
          promise.reject("Calling GeminiX failed", error)
        }, inputText, modelImages)
    } catch (e: Exception) {
      promise.reject("Invoking countChatTokens failed", e)
    }
  }

  @ReactMethod
  fun getChatHistory(promise: Promise) {
    try {
      GeminiX.getChatHistory(
        { history ->
          val result = Arguments.createArray()
          for (item in history) {
            val itemMap = Arguments.createMap()
            itemMap.putBoolean("isUser", item.isUser)
            val partsMap = Arguments.createArray()
            for (part in item.parts) {
              val partMap = Arguments.createMap()
              when (part) {
                is TextHistoryPart -> {
                  partMap.putString("type", "text")
                  partMap.putString("content", part.content)
                }
                is ImageHistoryPart -> {
                  partMap.putString("type", "image/bitmap")
                  partMap.putString("content", GeminiX.bitmapToBase64(part.content))
                }
                is BlobHistoryPart -> {
                  partMap.putString("type", part.mimeType)
                  val contentString = Base64.encodeToString(part.content, Base64.DEFAULT)
                  partMap.putString("content", contentString)
                }
              }
              partsMap.pushMap(partMap)
            }
            itemMap.putArray("parts", partsMap)
            result.pushMap(itemMap)
          }
          val historyMap = Arguments.createMap()
          historyMap.putArray("history", result)
          promise.resolve(historyMap)
        },
        { error ->
          promise.reject("Calling GeminiX failed", error)
        }
      )
    } catch (e: Exception) {
      promise.reject("Invoking getChatHistory failed", e)
    }
  }

  /*************************************************************************
   * Internal Methods
   ************************************************************************/
  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  /*************************************************************************
   * Companion
   ************************************************************************/
  companion object {
    const val NAME = "GeminiX"
  }
}
