import React, { useState, useEffect, useCallback } from "react";
import { Form, useSubmit, useLocation } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClockRotateLeft, faXmark, faTrash } from "@fortawesome/free-solid-svg-icons";
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import { useRef } from "react";

export default function SearchBar({ shadow = true, value = "" }) {
    const [query, setQuery] = useState(value);
    const [suggestions, setSuggestions] = useState([]);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const submit = useSubmit();
    const [isFocused, setIsFocused] = useState(false);
    const inputRef = useRef(null);
    const suggestionBoxRef = useRef(null);
    const location = useLocation();

    useEffect(() => {
        inputRef.current?.blur(); // Remove focus on route change
        setSelectedIndex(-1);
    }, [location]);

    const filterSuggestions = useCallback((searchQuery) => {
        const history = JSON.parse(localStorage.getItem("searchHistory") || "{}");

        // Calculate scores for sorting suggestions
        const calculateScore = (term, query) => {
            if (!query) return { score: history[term].lastUsed / 1000000, term };

            const termLower = term.toLowerCase();
            const queryLower = query.toLowerCase();

            const recency = history[term].lastUsed / 1000000;
            const frequency = Math.log(history[term].count + 1) * 10; // to avoid biasing towards very frequent terms

            let matchScore = 0;
            if (termLower === queryLower) {
                matchScore = 100;
            } else if (termLower.startsWith(queryLower)) {
                matchScore = 75;
            } else if (termLower.includes(queryLower)) {
                matchScore = 50;

                const position = termLower.indexOf(queryLower);
                if (position <= 3) matchScore += 10;
            }

            const occurrences = (termLower.match(new RegExp(queryLower, "g")) || []).length;
            if (occurrences > 1) matchScore += occurrences * 5;

            return {
                score: matchScore * 1000 + frequency * 10 + recency,
                term,
            };
        };

        let suggestions = [];

        if (!searchQuery) {
            suggestions = Object.keys(history)
                .filter((term) => term.trim() !== "")
                .map((term) => ({
                    term,
                    score: history[term].lastUsed + history[term].count * 10000,
                }))
                .sort((a, b) => b.score - a.score)
                .map((item) => item.term)
                .slice(0, 5);
        } else {
            suggestions = Object.keys(history)
                .filter((term) => term.trim() !== "" && term.toLowerCase().includes(searchQuery.toLowerCase()))
                .map((term) => calculateScore(term, searchQuery))
                .sort((a, b) => b.score - a.score)
                .map((item) => item.term)
                .slice(0, 5);
        }

        setSuggestions(suggestions);
    }, []);

    useEffect(() => {
        filterSuggestions(query);
    }, [query, filterSuggestions]);

    const handleInputChange = (e) => {
        setQuery(e.target.value);
        setSelectedIndex(-1);
    };

    const handleSuggestionClick = (suggestion) => {
        if (!suggestion.trim()) return;
        setIsFocused(false);
        setQuery(suggestion);

        const formData = new FormData();
        formData.append("query", suggestion);
        submit(formData, { method: "post" });
    };

    const handleFormSubmit = (e) => {
        if (!query.trim()) {
            e.preventDefault();
            return;
        }

        const history = JSON.parse(localStorage.getItem("searchHistory") || "{}");
        if (!history[query]) {
            history[query] = {
                lastUsed: Date.now(),
                count: 1,
            };
        } else {
            history[query].lastUsed = Date.now();
            history[query].count += 1;
        }
        localStorage.setItem("searchHistory", JSON.stringify(history));
    };

    const handleFocus = () => {
        setIsFocused(true);
        inputRef.current.focus();
    };

    const handleBlur = (e) => {
        if (!suggestionBoxRef.current?.contains(e.relatedTarget)) {
            setIsFocused(false);
        }
    };

    const handleKeyDown = (e) => {
        if (!isFocused || suggestions.length === 0) return;

        if (e.key === "ArrowDown") {
            e.preventDefault();
            setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : 0));
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setSelectedIndex((prev) => (prev > 0 ? prev - 1 : suggestions.length - 1));
        } else if (e.key === "Enter" && selectedIndex >= 0) {
            e.preventDefault();
            handleSuggestionClick(suggestions[selectedIndex]);
        } else if (e.key === "Escape") {
            setIsFocused(false);
            inputRef.current.blur();
        }
    };

    const clearSearchHistory = (e) => {
        e.stopPropagation();
        localStorage.setItem("searchHistory", "{}");
        setSuggestions([]);
    };

    const highlightMatch = (text, query) => {
        if (!query) return text;

        const lowerText = text.toLowerCase();
        const lowerQuery = query.toLowerCase();
        const index = lowerText.indexOf(lowerQuery);

        if (index === -1) return text;

        return (
            <>
                {text.substring(0, index)}
                <span className="font-bold text-indigo-600">{text.substring(index, index + query.length)}</span>
                {text.substring(index + query.length)}
            </>
        );
    };

    return (
        <div className="relative w-full">
            <Form method="post" className="w-full space-y-4" onSubmit={handleFormSubmit}>
                <div className="flex w-full space-x-2 h-12 items-start">
                    <ul className={"border-zinc-300 border-1 flex-1 rounded-3xl bg-white" + (shadow ? " shadow-md" : "")} onFocus={handleFocus} onBlur={handleBlur} onClick={handleFocus} tabIndex="0" ref={suggestionBoxRef}>
                        <li className="px-4 py-2 flex items-center space-x-2 h-12">
                            <FontAwesomeIcon icon={faMagnifyingGlass} className="bg-transparent text-zinc-800 text-md" />
                            <input name="query" type="text" placeholder="Search anything..." value={query} onChange={handleInputChange} onKeyDown={handleKeyDown} className="focus:outline-none w-full" ref={inputRef} />
                            {query && (
                                <button type="button" onClick={() => setQuery("")} className="ml-2 text-zinc-500 hover:text-zinc-700 focus:outline-none cursor-pointer">
                                    <FontAwesomeIcon icon={faXmark} className="text-zinc-800 text-md" />
                                </button>
                            )}
                        </li>
                        {isFocused && suggestions.length > 0 && (
                            <>
                                <li className="border-t-1 mx-4 pt-1 border-zinc-200 flex justify-between items-center">
                                    <span className="text-xs text-gray-500 px-2 py-1">Recent Searches</span>
                                    <button type="button" onClick={clearSearchHistory} className="text-xs text-gray-500 px-2 py-1 hover:text-red-500 flex items-center gap-1 cursor-pointer">
                                        <FontAwesomeIcon icon={faTrash} /> Clear
                                    </button>
                                </li>
                                {suggestions.map((s, i) => (
                                    <li key={i} onClick={() => handleSuggestionClick(s)} className={`px-4 py-2 h-12 hover:bg-zinc-100 cursor-pointer flex items-center space-x-2 ${selectedIndex === i ? "bg-zinc-100" : ""}`} onMouseEnter={() => setSelectedIndex(i)}>
                                        <FontAwesomeIcon icon={faClockRotateLeft} className="text-zinc-800 text-md" />
                                        <p>{highlightMatch(s, query)}</p>
                                    </li>
                                ))}
                            </>
                        )}
                    </ul>
                    <button type="submit" className="bg-zinc-800 text-zinc-100 pl-2 pr-4 py-2 rounded-full cursor-pointer h-12">
                        <FontAwesomeIcon icon={faMagnifyingGlass} className="text-zinc-100 text-md px-2" />
                        Search
                    </button>
                </div>
            </Form>
        </div>
    );
}
