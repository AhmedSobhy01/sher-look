import React from "react";

export default function SkeletonLoader({ count = 4 }) {
    return (
        <div className="space-y-6">
            {Array(count)
                .fill(0)
                .map((_, index) => (
                    <div key={index} className="animate-pulse">
                        <div className="h-6 bg-gray-200 rounded-md w-3/4 mb-2"></div>
                        <div className="h-4 bg-emerald-100 rounded-md w-1/2 mb-3"></div>
                        <div className="space-y-2">
                            <div className="h-4 bg-gray-100 rounded-md w-full"></div>
                            <div className="h-4 bg-gray-100 rounded-md w-5/6"></div>
                        </div>
                    </div>
                ))}
        </div>
    );
}
