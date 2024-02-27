import { NativeModules, Platform } from 'react-native';

import type {
  ModelParams,
  PluginSendMessageOptions,
  GeminiXResponseChunk,
  PluginCountTokensOptions,
  GeminiXResponseCount,
  PluginChatHistoryItem,
  PluginCountChatTokensOptions,
  ModelChatHistoryItem,
} from './lib/GeminiXTypes';

/**************************************************************************
 * Internal Constants
 **************************************************************************/

const LINKING_ERROR =
  `The package 'react-native-gemini-x' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const GeminiX = NativeModules.GeminiX
  ? NativeModules.GeminiX
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**************************************************************************
 * Plugin Methods
 **************************************************************************/
export function initModel(params: ModelParams): Promise<void> {
  return GeminiX.initModel(params);
}

export function sendMessage(
  inputText: string,
  options?: PluginSendMessageOptions
): Promise<GeminiXResponseChunk> {
  return GeminiX.sendMessage(inputText, options);
}

export function countTokens(
  inputText: string,
  options?: PluginCountTokensOptions
): Promise<GeminiXResponseCount> {
  return GeminiX.countTokens(inputText, options);
}

export function initChat(chatHistory?: PluginChatHistoryItem[]) {
  return GeminiX.initChat(chatHistory);
}

export function sendChatMessage(
  inputText: string,
  options?: PluginSendMessageOptions
): Promise<GeminiXResponseChunk> {
  return GeminiX.sendChatMessage(inputText, options);
}

export function countChatTokens(
  inputText: string,
  options?: PluginCountChatTokensOptions
): Promise<GeminiXResponseCount> {
  return GeminiX.countTokens(inputText, options);
}

export function getChatHistory(): Promise<ModelChatHistoryItem> {
  return GeminiX.getChatHistory();
}
