import React, { useState, useRef, useEffect } from "react";
import { ChevronDownIcon } from "@heroicons/react/24/outline";

interface AutocompleteOption {
  value: string;
  label: string;
  category?: string;
}

interface AutocompleteProps {
  label?: string;
  options: AutocompleteOption[];
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
  groupBy?: (option: AutocompleteOption) => string;
}

const Autocomplete: React.FC<AutocompleteProps> = ({
  label,
  options,
  value,
  onChange,
  placeholder = "검색하거나 선택하세요...",
  error,
  disabled = false,
  groupBy,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 선택된 값의 label 찾기
  const selectedOption = options.find((opt) => opt.value === value);
  const displayValue = selectedOption ? selectedOption.label : "";

  // 검색 필터링
  const filteredOptions = options.filter(
    (option) =>
      option.label.toLowerCase().includes(searchValue.toLowerCase()) ||
      option.value.toLowerCase().includes(searchValue.toLowerCase())
  );

  // 그룹화된 옵션
  const groupedOptions = groupBy
    ? filteredOptions.reduce((acc, option) => {
        const group = groupBy(option);
        if (!acc[group]) acc[group] = [];
        acc[group].push(option);
        return acc;
      }, {} as Record<string, AutocompleteOption[]>)
    : null;

  // 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
        setSearchValue("");
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // 키보드 네비게이션
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlightedIndex((prev) =>
        prev < filteredOptions.length - 1 ? prev + 1 : prev
      );
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (highlightedIndex >= 0 && highlightedIndex < filteredOptions.length) {
        handleSelect(filteredOptions[highlightedIndex]);
      }
    } else if (e.key === "Escape") {
      setIsOpen(false);
      setSearchValue("");
    }
  };

  const handleSelect = (option: AutocompleteOption) => {
    onChange(option.value);
    setIsOpen(false);
    setSearchValue("");
    setHighlightedIndex(-1);
  };

  const handleInputClick = () => {
    if (!disabled) {
      setIsOpen(true);
      inputRef.current?.select();
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
    setIsOpen(true);
    setHighlightedIndex(-1);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}

      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          className={`
            block w-full h-[38px] rounded-md border px-3 py-2 pr-10 text-sm
            focus:outline-none focus:ring-2 focus:ring-blue-500
            ${
              disabled
                ? "bg-gray-100 text-gray-400 cursor-not-allowed border-gray-200"
                : "bg-white text-gray-900 border-gray-300"
            }
            ${error ? "border-red-500" : ""}
          `}
          placeholder={placeholder}
          value={isOpen ? searchValue : displayValue}
          onChange={handleInputChange}
          onClick={handleInputClick}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />

        <button
          type="button"
          className="absolute inset-y-0 right-0 px-2 flex items-center z-10"
          onClick={() => setIsOpen(!isOpen)}
          disabled={disabled}
          tabIndex={-1}
        >
          <ChevronDownIcon
            className={`h-4 w-4 text-gray-400 transition-transform ${
              isOpen ? "rotate-180" : ""
            }`}
          />
        </button>
      </div>

      {/* 드롭다운 */}
      {isOpen && filteredOptions.length > 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg max-h-60 overflow-auto">
          {groupedOptions
            ? // 그룹화된 옵션
              Object.entries(groupedOptions).map(([group, groupOptions], groupIndex) => (
                <div key={`group-${groupIndex}-${group}`}>
                  <div className="px-3 py-2 text-xs font-semibold text-gray-500 bg-gray-50">
                    {group}
                  </div>
                  {groupOptions.map((option, index) => {
                    const globalIndex = filteredOptions.indexOf(option);
                    return (
                      <OptionItem
                        key={`grouped-${groupIndex}-${index}-${option.value}`}
                        option={option}
                        isHighlighted={highlightedIndex === globalIndex}
                        isSelected={value === option.value}
                        onClick={() => handleSelect(option)}
                        onMouseEnter={() => setHighlightedIndex(globalIndex)}
                      />
                    );
                  })}
                </div>
              ))
            : // 일반 옵션
              filteredOptions.map((option, index) => (
                <OptionItem
                  key={`option-${index}-${option.value || 'empty'}`}
                  option={option}
                  isHighlighted={highlightedIndex === index}
                  isSelected={value === option.value}
                  onClick={() => handleSelect(option)}
                  onMouseEnter={() => setHighlightedIndex(index)}
                />
              ))}
        </div>
      )}

      {/* 검색 결과 없음 */}
      {isOpen && searchValue && filteredOptions.length === 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg p-3 text-sm text-gray-500">
          검색 결과가 없습니다
        </div>
      )}

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
};

// 옵션 아이템 컴포넌트
const OptionItem: React.FC<{
  option: AutocompleteOption;
  isHighlighted: boolean;
  isSelected: boolean;
  onClick: () => void;
  onMouseEnter: () => void;
}> = ({ option, isHighlighted, isSelected, onClick, onMouseEnter }) => {
  return (
    <div
      className={`
        px-3 py-2 cursor-pointer text-sm transition-colors
        ${isHighlighted ? "bg-blue-50" : ""}
        ${isSelected ? "bg-blue-100 font-medium" : ""}
        hover:bg-blue-50
      `}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
    >
      <div className="flex items-center justify-between">
        <span>{option.label}</span>
      </div>
    </div>
  );
};

export default Autocomplete;
