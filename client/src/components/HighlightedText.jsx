import React from "react";

const HighlightedText = ({ text }) => {
    const renderHighlightedText = () => {
        if (!text) return null;

        const regex = /<b>(.*?)<\/b>/g;
        let lastIndex = 0;
        const result = [];
        let match;
        let i = 0;

        while ((match = regex.exec(text)) !== null) {
            if (match.index > lastIndex) {
                result.push(<span key={`plain-${i}`}>{text.substring(lastIndex, match.index)}</span>);
                i++;
            }

            result.push(<strong key={`bold-${i}`}>{match[1]}</strong>);
            i++;

            lastIndex = regex.lastIndex;
        }

        if (lastIndex < text.length) result.push(<span key={`plain-${i}`}>{text.substring(lastIndex)}</span>);

        return result;
    };

    return <>{renderHighlightedText()}</>;
};

export default HighlightedText;
