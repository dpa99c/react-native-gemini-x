import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  SafeAreaView,
  ScrollView,
  StatusBar,
} from 'react-native';

import config from './config';
import { initModel } from 'react-native-gemini-x';

export default function App() {
  // Constants
  const DEFAULT_MODEL_NAME = config.DEFAULT_MODEL_NAME;

  // State (Refs)
  const [modelName] = React.useState<string>(DEFAULT_MODEL_NAME);
  const [logMessages, setLogMessages] = React.useState<string[]>([]);

  // Effects (onMount)
  React.useEffect(() => {
    initModel({
      modelName: modelName,
      apiKey: config.GEMINI_API_KEY,
      ...config.generativeConfig,
    }).then(() => {
      log(`Model initialized: ${modelName}`);
    });
  }, [modelName]);

  function log(message: string) {
    setLogMessages((prev) => [...prev, message]);
  }

  // Template
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.scrollView}>
        <View style={styles.log}>
          {logMessages.map((message, index) => (
            <Text key={index}>{message}</Text>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: StatusBar.currentHeight,
  },
  scrollView: {
    marginHorizontal: 20,
  },
  log: {
    padding: 10,
    margin: 10,
    borderWidth: 1,
    borderColor: 'black',
  },
});
