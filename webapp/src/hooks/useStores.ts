import React from "react"
import { stores } from "../stores"

const useStores = () => React.useContext(React.createContext<typeof stores>(stores))
export default useStores
