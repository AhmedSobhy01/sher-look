import React, { useState, useEffect } from "react";
import { Form, useSubmit, useLocation } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClockRotateLeft } from "@fortawesome/free-solid-svg-icons";
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import { useRef } from "react";

export default function SearchBar({ shadow = true, value = "" }) {
    const [query, setQuery] = useState(value);
    const [suggestions, setSuggestions] = useState([]);
    const submit = useSubmit();
    const [isFocused, setIsFocused] = useState(false);
    const inputRef = useRef(null);
    const suggestionBoxRef = useRef(null);
    const location = useLocation();

    useEffect(() => {
        inputRef.current?.blur(); // Remove focus on route change
    }, [location]);

    useEffect(() => {
        const history = JSON.parse(localStorage.getItem("searchHistory") || "{}");
        const sorted = Object.entries(history)
            .sort(([, a], [, b]) => b - a)
            .filter(([q]) => q.trim() !== "")
            .map(([q]) => q);

        if (!query) {
            // Show most recent 5 searches
            setSuggestions(sorted.slice(0, 5));
        } else {
            // Show matches starting with query
            const matches = sorted.filter((q) => q.toLowerCase().startsWith(query.toLowerCase())).slice(0, 5);
            setSuggestions(matches);
        }
    }, [query]);

    const handleInputChange = (e) => {
        setQuery(e.target.value);
    };

    const handleSuggestionClick = (suggestion) => {
        if (!suggestion.trim()) return;

        console.log("Suggestion clicked:", suggestion);
        setIsFocused(false);

        setQuery(suggestion);
        // Immediately submit the form with the selected suggestion
        const formData = new FormData();
        formData.append("query", suggestion);
        submit(formData, { method: "post" });
    };

    const handleFormSubmit = (e) => {
        if (!query.trim()) {
            e.preventDefault();
            return;
        }

        // Save query in localStorage before submitting
        const history = JSON.parse(localStorage.getItem("searchHistory") || "{}");
        history[query] = (history[query] || 0) + 1;
        localStorage.setItem("searchHistory", JSON.stringify(history));
    };

    const handleFocus = () => {
        setIsFocused(true);
        inputRef.current.focus();
    };

    const handleBlur = (e) => {
        if (!suggestionBoxRef.current.contains(e.relatedTarget)) {
            setIsFocused(false);
        }
    };

    return (
        <div className="relative w-full">
            <Form method="post" className="w-full space-y-4" onSubmit={handleFormSubmit}>
                <div className="flex w-full space-x-2 h-12 items-start">
                    <ul
                        className={"border-zinc-300 border-1 flex-1 rounded-3xl bg-white" + (shadow ? " shadow-md" : "")}
                        onFocus={handleFocus}
                        onBlur={handleBlur}
                        onClick={handleFocus}
                        tabIndex="0" // Make the div focusable
                        ref={suggestionBoxRef}
                    >
                        <li className="px-4 py-2 cursor-pointer flex items-center space-x-2 h-12">
                            <FontAwesomeIcon icon={faMagnifyingGlass} className="bg-transparent text-zinc-800 text-md" />
                            <input name="query" type="search" placeholder="Search anything..." value={query} onChange={handleInputChange} className="focus:outline-none w-full" ref={inputRef} />
                        </li>
                        {isFocused && suggestions.length > 0 && (
                            <>
                                <li className="border-t-1 mx-4 border-zinc-200"></li>
                                {suggestions.map((s, i) => (
                                    <li key={i} onClick={() => handleSuggestionClick(s)} className="px-4 py-2 h-12 hover:bg-zinc-100 cursor-pointer rounded-full flex items-center space-x-2">
                                        <FontAwesomeIcon icon={faClockRotateLeft} className="text-zinc-800 text-md" />
                                        <p>{s}</p>
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
