import React, { useState, useEffect } from "react";
import SearchBar from "@/components/SearchBar";
import { useNavigate, useSearchParams } from "react-router";
import Logo from "@/assets/Logo.png";
import HighlightText from "@/components/HighlightedText";
import { redirect } from "react-router-dom";
import SkeletonLoader from "@/components/SkeletonLoader";

const ResultsPage = function () {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [data, setData] = useState({ data: [], timeTaken: 0, query: "" });
    const [isLoading, setIsLoading] = useState(true);
    const [isLoaded, setIsLoaded] = useState(false);
    const [showResults, setShowResults] = useState(false);

    useEffect(() => {
        const searchTerm = searchParams.get("query");

        if (!searchTerm || searchTerm.trim() === "") {
            navigate("/");
            return;
        }

        const fetchData = async () => {
            setIsLoading(true);

            const dummyResponse = {
                query: searchTerm,
                timeTaken: Math.floor(Math.random() * 2000) + 100,
                data: [
                    {
                        id: 2,
                        Url: "https://www.google.com",
                        title: "Google",
                        description: "Search the world's information, including webpages, images, videos and more. Google has many special features to help you find exactly what you're looking for.",
                        highlights: ["search", "information", "webpages", "images", "videos", "features", "find", "looking"],
                    },
                    {
                        id: 1,
                        Url: "https://www.bing.com",
                        title: "Bing",
                        description: "Bing helps you turn information into action, making it faster and easier to go from searching to doing.",
                        highlights: ["information", "action", "making", "faster", "easier", "searching", "doing"],
                    },
                    {
                        id: 4,
                        Url: "https://www.duckduckgo.com",
                        title: "DuckDuckGo",
                        description: "The Internet privacy company that empowers you to seamlessly take control of your personal information online, without any tradeoffs.",
                        highlights: ["Internet", "privacy", "company", "empowers", "seamlessly", "control", "personal", "information", "online"],
                    },
                    {
                        id: 3,
                        Url: "https://www.yahoo.com",
                        title: "Yahoo",
                        description: "News, email and search are just the beginning. Discover more every day. Find your yodel.",
                        highlights: ["News", "email", "search", "beginning", "Discover", "yodel"],
                    },
                ],
            };

            setData(dummyResponse);
            setIsLoading(false);
        };

        fetchData();
    }, [searchParams, navigate]);

    useEffect(() => {
        setIsLoaded(true);
        if (!isLoading) {
            const timer = setTimeout(() => setShowResults(true), 300);
            return () => clearTimeout(timer);
        } else {
            setShowResults(false);
        }
    }, [isLoading]);

    if (data.data && data.data.length > 0) {
        data.data.sort((a, b) => a.id - b.id);
    }

    return (
        <div className="bg-gray-50 min-h-screen">
            <header className={`flex items-center justify-left mb-6 shadow-sm py-4 px-8 bg-white transition-all duration-500 ${isLoaded ? "opacity-100" : "opacity-0"}`}>
                <img src={Logo} className="h-12 w-auto object-cover cursor-pointer ml-8 hidden sm:block mr-2 md:mr-4 lg:mr-8 md:ml-12 lg:ml-0 hover:scale-105 transition-transform" onClick={() => navigate("/")} />
                <div className="max-w-2xl w-full">
                    <SearchBar shadow={false} value={searchParams.get("query")} />
                </div>
            </header>

            <main className="max-w-3xl mr-8 ml-8 sm:ml-16 md:ml-20 lg:ml-42 pb-12">
                {isLoading ? (
                    <>
                        <div className="animate-pulse h-4 bg-gray-200 rounded w-48 mb-6"></div>
                        <SkeletonLoader count={5} />
                    </>
                ) : (
                    <>
                        <p className={`text-gray-600 text-sm mb-6 transition-all duration-300 ${isLoaded ? "animate-fade-in" : "opacity-0"}`}>
                            About <span className="font-medium">{data.data.length}</span> results
                            <span className="font-medium">
                                {" "}
                                "{data.query}" took <span className="font-semibold">{data.timeTaken < 100 ? `${data.timeTaken}ms` : `${(data.timeTaken / 1000).toFixed(2)}s`}</span>
                            </span>
                        </p>

                        <ul className="space-y-6">
                            {data.data.map((result, index) => (
                                <li key={result.Url} className={`card ${showResults ? "animate-slide-up" : "opacity-0 translate-y-8"}`} style={{ animationDelay: `${index * 100}ms` }}>
                                    <a href={result.Url} target="_blank" rel="noopener noreferrer" className="block group">
                                        <p className="text-xl font-semibold group-hover:text-blue-600 transition-colors duration-200">{result.title}</p>
                                        <p className="text-emerald-600 text-sm truncate mt-1">{result.Url}</p>
                                    </a>
                                    <div className="mt-2">
                                        <HighlightText text={result.description} highlights={result.highlights} className="text-gray-600 text-sm leading-relaxed" />
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </>
                )}
            </main>
        </div>
    );
};

ResultsPage.action = async function ({ request }) {
    let formData = await request.formData();
    let query = formData.get("query");
    if (query) return redirect(`/search?query=${query}`);
};

export default ResultsPage;
