import React from "react";
export default function HighlightedText({ text, highlights, ...rest }) {
  const regex = new RegExp(`(${highlights.join("|")})`, "gi");
  const parts = text.split(regex);

  return (
    <p {...rest}>
      {parts.map((part, index) =>
        highlights.includes(part) ? (
          <span key={index} className="font-bold">
            {part}
          </span>
        ) : (
          part
        )
      )}
    </p>
  );
}
