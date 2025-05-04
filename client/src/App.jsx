import Homepage from "./pages/Homepage";
import {
  createBrowserRouter,
  RouterProvider,
  Route,
  Link,
} from "react-router-dom";
import ResultsPage from "./pages/ResultsPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Homepage />,
    action: Homepage.action,
  },
  {
    path: "/search",
    element: <ResultsPage />,
    loader: ResultsPage.loader,
    action: ResultsPage.action,
  },
]);

function App() {
  return <RouterProvider router={router} />;
}

export default App;
