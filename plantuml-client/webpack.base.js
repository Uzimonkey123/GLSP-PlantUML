const path = require("path");

module.exports = {
  resolve: {
    extensions: [ ".ts", ".tsx", ".js", ".json", ".css" ]
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: "ts-loader",
        exclude: /node_modules/
      },
      {
        test: /\.css$/i,
        use: [ "style-loader", "css-loader" ]
      },
      {
        test: /\.(woff2?|ttf|eot|svg)$/i,
        type: "asset/inline"
      }
    ]
  }
};